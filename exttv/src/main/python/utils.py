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

def set_plugin_name(plugin_name):
    plugin.plugin_name = plugin_name

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

def reload_module(module_name):
    if module_name in globals():
        importlib.reload(globals()[module_name])
    else:
        globals()[module_name] = importlib.import_module(module_name)

def run(argv):
    print(argv)
    plugin_name = argv[0].split("/")[2]

    # Remove paths starting with .//exttv_addon/
    sys.path = [path for path in sys.path if not path.startswith(full_addons_path())]

    sys.path.append(os.path.join(full_addons_path(), plugin_name))
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

    reload_module(library.replace('.py',''))
    return plugin._to_return_items
