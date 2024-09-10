import re
import os
import sys
import shutil
import requests
import base64
import zipfile
import urllib.parse
import json
import importlib
import traceback

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

def reload_module(module_name, plugin_name):
    # Define the full addons path
    addons_path = full_addons_path()
    
    # Remove paths starting with .//exttv_addon/
    sys.path = [path for path in sys.path if not path.startswith(addons_path)]
    
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
    
    # Add the new plugin path
    plugin_path = os.path.join(addons_path, plugin_name)
    sys.path.append(plugin_path)
    
    # Import the module if it exists
    module = importlib.import_module(module_name)
    return module

def run(argv):
    print(argv)
    plugin_name = argv[0].split("/")[2]
    plugin.plugin_name = plugin_name
    sys.argv = argv

    tree = ET.parse(os.path.join(full_addons_path(), plugin_name)+'/addon.xml')
    root = tree.getroot()
    # Find the extension with the specific point attribute
    for extension in root.findall('extension'):
        if extension.get('point') == 'xbmc.python.pluginsource':
            library = extension.get('library')
            break
    else:
        raise Exception("Failed to find the library attribute in addon.xml")

    plugin._to_return_items = []
    reload_module(library.replace('.py',''), plugin_name)
    return plugin._to_return_items
