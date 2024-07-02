import os
import platform
from datetime import datetime
import time
import utils

try:
    from android.content import Intent
    from java import jclass
    main_activity = jclass("com.android.exttv.MainActivity").getInstance()
except ImportError:
    main_activity = None

# Redefine the constants with their values
LOGDEBUG = 0
LOGINFO = 1
LOGNOTICE = 2
LOGWARNING = 3
LOGERROR = 4
LOGSEVERE = 5
LOGFATAL = LOGSEVERE
LOGNONE = 6

PLAYLIST_MUSIC = 0
PLAYLIST_VIDEO = 1

class KodiNavigationStack:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._path_stack = []
            cls._instance.items = []
            
        return cls._instance

    def __getitem__(self, key):
        return self.items[key]


def sleep(milliseconds):
    time.sleep(milliseconds / 1000)

def executebuiltin(command, wait=False):
    print(f"Pretending to execute builtin: {command}")
    return True

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

class Player():

    def play(self, playlist, xlistitem):
        print(playlist, xlistitem)
        intent = Intent(main_activity.getApplicationContext(), jclass("com.android.exttv.PlayerActivity"))
        main_activity.startActivity(intent)

    def isPlaying(self):
        return True

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


class PlayList:

    def __init__(self, playList):
        if playList not in [PLAYLIST_MUSIC, PLAYLIST_VIDEO]:
            raise ValueError("Invalid playlist type.")
        self.playlist_type = playList
        self.playlist_items = []

    def getPlayListId(self):
        return self.playlist_type

    def add(self, url, listitem=None, index=None):
        if index is None:
            self.playlist_items.append((url, listitem))
        else:
            self.playlist_items.insert(index, (url, listitem))

    def load(self, filename):
        try:
            # Code to load playlist from filename
            return True
        except Exception as e:
            print(f"Error loading playlist: {e}")
            return False

    def remove(self, filename):
        self.playlist_items = [(url, item) for url, item in self.playlist_items if url != filename]

    def clear(self):
        self.playlist_items = []

    def size(self):
        return len(self.playlist_items)

    def shuffle(self):
        import random
        random.shuffle(self.playlist_items)

    def unshuffle(self):
        # Reset to original order
        pass

    def getposition(self):
        # Return current position
        pass

class Monitor:
    def __init__(self):
        pass

    def onSettingsChanged(self):
        pass

    def onScreensaverActivated(self):
        pass

    def onScreensaverDeactivated(self):
        pass

    def onDPMSActivated(self):
        pass

    def onDPMSDeactivated(self):
        pass

    def onScanStarted(self, library):
        pass

    def onScanFinished(self, library):
        pass

    def onCleanStarted(self, library):
        pass

    def onCleanFinished(self, library):
        pass

    def onNotification(self, sender, method, data):
        pass

    def waitForAbort(self, timeout=None):
        return True

    def abortRequested(self):
        return True