import os
import sys
import utils

utils.addons_path = os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../addons')) # very important to normalize
if not os.path.exists(utils.addons_path):
    os.mkdir(utils.addons_path)

kod_folder_name = 'plugin.video.kod'
url = 'https://github.com/kodiondemand/addon/archive/refs/heads/master.zip'
utils.download_and_extract_plugin(url, kod_folder_name, False)

sys.path.append(os.path.join(utils.addons_path, kod_folder_name))

sys.argv = [None,3,""]
import default
# print(default.__file__)
# from platformcode import config
# import platformcode
# try:
#     import platformcode
#     print("Module imported successfully")
# except Exception as e:
#     print(f"Error importing module: {e}")

