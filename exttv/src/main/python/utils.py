import os
import shutil
import requests
import zipfile

addons_path = None

class DownloadError(Exception):
    pass

class ExtractionError(Exception):
    pass

def download_and_extract_plugin(url, plugin_name, force=False):
    try:
        if force or not os.path.exists(os.path.join(addons_path, plugin_name)):
            zip_file = os.path.join(addons_path, 'master.zip')
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
                    zip_ref.extractall(addons_path)

                extracted_folder = os.path.join(addons_path, 'addon-master')
                if os.path.exists(os.path.join(addons_path, plugin_name)):
                    # Remove existing folder before renaming
                    shutil.rmtree(os.path.join(addons_path, plugin_name))

                os.rename(extracted_folder, os.path.join(addons_path, plugin_name))
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