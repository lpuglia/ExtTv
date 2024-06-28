import os
import platform
from datetime import datetime
import time
import utils

# Redefine the constants with their values
LOGDEBUG = 0
LOGINFO = 1
LOGNOTICE = 2
LOGWARNING = 3
LOGERROR = 4
LOGSEVERE = 5
LOGFATAL = LOGSEVERE
LOGNONE = 6

def sleep(milliseconds):
    time.sleep(milliseconds / 1000)

def executebuiltin(command):
    print(f"Pretending to execute builtin: {command}")

def translatePath(path):
    if 'special://' in path:
        if path.startswith('special://profile/'):
            return path.replace('special://profile/', utils.full_userdata_path())
        else:
            return path.replace('special://', utils.full_home_path())
    else:
        return path

def getSkinDir():
    # Dummy implementation to return a fixed skin directory
    return 'skin.estuary'

def getInfoLabel(label):
    # Dummy implementation to return a fixed value based on the label
    info_labels = {
        "System.BuildVersion": "18.9 Leia",
    }
    return info_labels.get(label, "Unknown Label")

def executeJSONRPC(jsonrpc_request):
    response_dict = {}
    response_dict['{"jsonrpc": "2.0", "method": "Settings.GetSettingValue", "params": {"setting": "lookandfeel.skin"}, "id": 1 }'] = '{"id":1,"jsonrpc":"2.0","result":{"value":"skin.estuary"}\}'
    if jsonrpc_request in response_dict:
        return response_dict[jsonrpc_request]
    else:
        return None

class Logger:
    def __init__(self):
        self.log_file = os.path.join(utils.full_home_path(), 'kodi.log')
        # Ensure the log directory exists
        os.makedirs(os.path.dirname(self.log_file), exist_ok=True)
    
    def __call__(self, level, message):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(self.log_file, 'a') as file:
            log_entry = f"{timestamp} - {level}: {message}\n"
            file.write(log_entry)

log = Logger()

class Player():
    pass

def getCondVisibility(condition):
    parts = condition.split('.')
    if len(parts) < 2:
        return False  # Invalid condition format

    if parts[0] == 'system' and parts[1] == 'platform':
        if len(parts) > 2:
            platform_name = parts[2]
            current_platform = platform.system().lower()
            return current_platform == platform_name.lower()
        else:
            return False  # Incomplete condition (missing platform name)
    else:
        return False  # Unknown condition type
