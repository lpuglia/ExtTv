import os
import shutil
import requests
import base64
import zipfile
import urllib.parse
import json

workspace = None
home_path = 'kodi_home/'
addons_path = 'kodi_home/addons/'
userdata_path = 'kodi_home/userdata/'
addondata_path = 'kodi_home/userdata/addon_data/'
database_path = 'kodi_home/userdata/Database/'

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

def download_and_extract_plugin(url, plugin_name, force=False):
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

                extracted_folder = os.path.join(full_addons_path(), 'addon-master')
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
