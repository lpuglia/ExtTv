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

class DownloadError(Exception):
    pass

class ExtractionError(Exception):
    pass

def get_from_git(url, force=False):
    user, repo, branch = url.split('/')
    addon_xml_url = f"https://raw.githubusercontent.com/{user}/{repo}/{branch}/addon.xml"
    zip_path = f"https://github.com/{user}/{repo}/archive/refs/heads/{branch}.zip"
    try:
        response = requests.head(addon_xml_url)
        response.raise_for_status()  # Raise an HTTPError if the HTTP request returned an unsuccessful status code
        addon_xml = requests.get(addon_xml_url)
    except requests.exceptions.RequestException as e:
        raise Exception(f"Error occurred while checking URL: {e}")
    match = re.search(r'id="([^"]+)"', addon_xml.text)
    if match:
        plugin_name = match.group(1)
    else:
        raise Exception("Failed to get plugin id")
    return download_and_extract_plugin(zip_path, plugin_name, force)

def get_from_repository(url, force=False):
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        zip_path = data.get('result').get('data').get('addon').get('platforms')[0].get('path')
        plugin_name = data.get('result').get('data').get('addon').get('addonid')
    except (requests.RequestException, json.JSONDecodeError) as e:
        raise Exception("Failed to fetch or decode JSON data")
        # main_activity.showToast(str(e), 1)
    return download_and_extract_plugin(zip_path, plugin_name, force)

def get_top_level_folder_name(zip_file):
    with zipfile.ZipFile(zip_file, 'r') as zip_ref:
        # List all files in the zip
        all_files = zip_ref.namelist()

        # Filter to get only the top-level directories
        top_level_dirs = set()
        for file in all_files:
            parts = file.split('/')
            if len(parts) > 1:
                top_level_dirs.add(parts[0])

        # Assuming the ZIP contains a single top-level directory
        if len(top_level_dirs) == 1:
            return top_level_dirs.pop()
        else:
            return None

def download_and_extract_plugin(url, plugin_name, force=False):
    filename = url.split('/')[-1]
    try:
        if force or not os.path.exists(os.path.join(full_addons_path(), plugin_name)):
            response = requests.get(url)
            zip_file = os.path.join(full_addons_path(), filename)

            if response.status_code == 200:
                # Write the contents to a file
                with open(zip_file, 'wb') as f:
                    f.write(response.content)
                print(f"Downloaded {zip_file} successfully")
            else:
                raise DownloadError(f"Failed to download {url}. Status code: {response.status_code}")

            # Extract the contents of the ZIP file
            try:
                if os.path.exists(os.path.join(full_addons_path(), plugin_name)):
                    # Remove existing folder before renaming
                    shutil.rmtree(os.path.join(full_addons_path(), plugin_name))

                with zipfile.ZipFile(zip_file, 'r') as zip_ref:
                    zip_ref.extractall(full_addons_path())
                extracted_folder = os.path.join(full_addons_path(), get_top_level_folder_name(zip_file))
                # print(extracted_folder)

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
        traceback.print_exc()
        raise

    os.makedirs(os.path.join(full_addondata_path(), plugin_name), exist_ok=True)
    plugin.plugin_name = plugin_name
    return plugin_name

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
