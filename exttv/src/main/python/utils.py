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

class DownloadError(Exception):
    pass

class ExtractionError(Exception):
    pass

def get_default_branch(user, repo):
    url = f"https://api.github.com/repos/{user}/{repo}"
    response = requests.get(url)

    if response.status_code == 200:
        data = response.json()
        return data.get('default_branch', None)
    else:
        print(f"Error: {response.status_code}")
        print(response.json())
        return None

def download_and_extract_plugin(user, repo, force=False):
    default_branch = get_default_branch(user, repo)
    addon_xml_url = f"https://raw.githubusercontent.com/{user}/{repo}/{default_branch}/addon.xml"
    url = f"https://github.com/{user}/{repo}/archive/refs/heads/{default_branch}.zip"
    addon_xml = requests.get(addon_xml_url)
    match = re.search(r'id="([^"]+)"', addon_xml.text)
    if match:
        plugin_name = match.group(1)
    else:
        raise Exception("Failed to get plugin id")
    try:
        if force or not os.path.exists(os.path.join(full_addons_path(), plugin_name)):
            zip_file = os.path.join(full_addons_path(), 'master.zip')
            response = requests.get(url)

            if response.status_code == 200:
                # Write the contents to a file
                with open(zip_file, 'wb') as f:
                    f.write(response.content)
                print(f"Downloaded {zip_file} successfully")
            else:
                raise DownloadError(f"Failed to download {url}. Status code: {response.status_code}")

            # Extract the contents of the ZIP file
            try:
                with zipfile.ZipFile(zip_file, 'r') as zip_ref:
                    zip_ref.extractall(full_addons_path())

                extracted_folder = os.path.join(full_addons_path(), f'{repo}-master')
                print(extracted_folder)
                if os.path.exists(os.path.join(full_addons_path(), plugin_name)):
                    # Remove existing folder before renaming
                    shutil.rmtree(os.path.join(full_addons_path(), plugin_name))

                os.rename(extracted_folder, os.path.join(full_addons_path(), plugin_name))
                os.remove(zip_file)
                print(f"Plugin {plugin_name} extracted successfully.")

            except zipfile.BadZipFile as e:
                raise ExtractionError(f"Failed to extract {zip_file}: {e}")
        else:
            print(f"Plugin {plugin_name} already exists. Skipping download.")

    except (requests.RequestException, DownloadError, ExtractionError, OSError) as e:
        # Handle specific exceptions
        print(f"Error occurred while downloading and extracting plugin: {e}")
        # Optionally, raise the exception to propagate it further
        raise

    os.makedirs(os.path.join(full_addondata_path(), plugin_name), exist_ok=True)
    plugin.plugin_name = plugin_name
    return plugin_name

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
    #argv = [f'plugin://{kod_folder_name}/', '3', argv2]
    plugin_name = argv[0].split("/")[2]
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
