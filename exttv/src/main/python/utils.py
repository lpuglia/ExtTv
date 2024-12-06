import os
import sys
import base64
import urllib.parse
import json
import importlib
import glob

import xml.etree.ElementTree as ET

import xbmc
from xbmc import Logger
from xbmcplugin import PluginRecorder
plugin = PluginRecorder()

workspace = None
home_path = 'exttv_home/'
addons_path = 'exttv_home/addons/'
userdata_path = 'exttv_home/userdata/'
addondata_path = 'exttv_home/userdata/addon_data/'
database_path = 'exttv_home/userdata/Database/'

last_uri = ''
parent_uri_map = {}

def full_home_path():
    return os.path.join(workspace, home_path)

def full_addons_path():
    return os.path.join(workspace, addons_path)

def full_userdata_path():
    return os.path.join(workspace, userdata_path)

def full_addondata_path():
    return os.path.join(workspace, addondata_path)

def full_database_path():
    return os.path.join(workspace, database_path)

def decode_plugin_path(plugin_path):
    parsed_url = urllib.parse.urlparse(plugin_path)
    query_params = urllib.parse.parse_qs(parsed_url.query)
    return json.loads(base64.b64decode(urllib.parse.unquote(parsed_url.query)).decode('utf-8'))

def init(path):
    global workspace 
    workspace = path
    os.makedirs(full_addons_path(), exist_ok=True)
    os.makedirs(full_addondata_path(), exist_ok=True)
    os.makedirs(full_database_path(), exist_ok=True)
    xbmc.log = Logger()
    print(f'kodi.log dumped in {workspace}')

def add_dependencies(module_path):
    root = ET.parse(module_path+'/addon.xml').getroot()
    requires = [addon.get('addon') for addon in root.find('requires').findall('import')]
    for require in requires:
        require_path = os.path.join(full_addons_path(), require)
        if 'xbmc.python' in require: continue

        if os.path.exists(require_path):
            # Extract unique subfolders containing at least one Python file
            subfolders = set()
            for file in glob.glob(os.path.join(require_path, '**', '*.py'), recursive=True):
                folder_path = os.path.dirname(file)
                if os.path.isfile(os.path.join(folder_path, '__init__.py')):
                    parent_dir = os.path.dirname(folder_path)
                    subfolders.add(parent_dir)
                else:
                    subfolders.add(folder_path)
            for f in subfolders: sys.path.append(f)
            add_dependencies(require_path)
        else:
            print(f"{require_path}: lib not found")

def reload_module(module_name, module_path):    
    # Remove paths with {home_path}
    sys.path = [path for path in sys.path if home_path not in path]
    
    # Clear the module cache
    importlib.invalidate_caches()
    
    # Filter out existing modules related to home_path
    filtered_modules = {}
    for k, v in sys.modules.items():
        if hasattr(v, '__file__') and v.__file__ and home_path in v.__file__:
            # print('Ignoring file:', k, v, v.__file__)
            continue
        elif hasattr(v, '__path__') and hasattr(v.__path__, '_path') and home_path in v.__path__._path[0]:
            # print('Ignoring path:', k, v, v.__path__)
            continue
        else:
            filtered_modules[k] = v
    sys.modules = filtered_modules
    
    sys.path.append(module_path) 
    add_dependencies(module_path)
    
    # import socks
    try:
        # import the addon and trigger call to it
        importlib.import_module(module_name)
    except SystemExit as e: # some addon try to exit when they finish the call, this prevents it
        print(f"Caught an exit attempt from {module_path.split('/')[-1]}")

def run(argv):
    global last_uri
    last_uri = argv[0]+argv[2]
    plugin_name = argv[0].split("/")[2]
    plugin.plugin_name = plugin_name
    sys.argv = argv

    module_path = os.path.join(full_addons_path(), plugin_name)
    root = ET.parse(module_path+'/addon.xml').getroot()
    # Find the extension with the specific point attribute
    for extension in root.findall('extension'):
        if extension.get('point') == 'xbmc.python.pluginsource':
            module_name = extension.get('library').replace('.py','')
            break
    else:
        raise Exception("Failed to find the library attribute in addon.xml")

    reload_module(module_name, module_path)
