import os
import re
import time
import json
import utils
import platform
import urllib.parse
from datetime import datetime
from types import SimpleNamespace

def serialize_namespace(obj):
    if isinstance(obj, SimpleNamespace):
        return obj.__dict__
    raise TypeError(f"Object of type {obj.__class__.__name__} is not JSON serializable")

try:
    from android.content import Intent
    from android.net import Uri
    from java import jclass
    PlayerActivity = jclass("com.android.exttv.PlayerActivity")
    main_activity = jclass("com.android.exttv.MainActivity").getInstance()
except ImportError as e:
     print("Could not import MainActivity", e)
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

def sleep(milliseconds):
    time.sleep(milliseconds / 1000)

def executebuiltin(command, wait=False):
    print(command)
    pattern = r"PlayMedia\(plugin://plugin.video.xbmctorrent/play/(.*?)\)"
    match = re.search(pattern, command)
    if match:
        magnet_encoded = match.group(1)
        magnet_decoded = urllib.parse.unquote(magnet_encoded)
        print("Magnet URI (Decoded):", magnet_decoded)
        main_activity.fireMagnetIntent(magnet_decoded)
    elif command.startswith('StartAndroidActivity'):
        main_activity.executeStartActivity(command)
    else:
        print(f"executebuiltin: pretending to execute builtin: {command}")
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
    response_dict['{"jsonrpc": "2.0", "method": "Settings.GetSettingValue", "params": {"setting": "lookandfeel.skin"}, "id": 1 }'] = '{"id":1,"jsonrpc":"2.0","result":{"value":"skin.estuary"}}'
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

def parse_piped_url(url):
    if url.endswith("|R{SSM}|"):
        url = url[:-8]
    if "|" in url:
        url, headers = url.split("|",1)
        headers = {k:v[0] for k,v in urllib.parse.parse_qs(headers).items()}
        return url, headers
    return url, {}

class Player():

    def play(self, playlist, xlistitem = None, windowed = False, startpos = -1):
        base_url = "exttv://app/?"
        if hasattr(playlist, 'playlist_type'):
            extra_info = playlist.playlist_items[0][1]
            url = playlist.playlist_items[0][0]
        else:
            extra_info = playlist
            url = playlist.path
        media_source = SimpleNamespace()
        print(extra_info)
        media_source.streamType = extra_info.mimetype
        media_source.source, media_source.headers = parse_piped_url(url)

        if media_source.streamType == "application/dash+xml":
            media_source.license = {}
            properties = extra_info.properties
            if "inputstream.adaptive.license_type" in properties:
                media_source.license["licenseType"] = properties["inputstream.adaptive.license_type"]
            if("inputstream.adaptive.license_key" in properties):
                media_source.license['licenseKey'], media_source.license['headers'] = parse_piped_url(properties['inputstream.adaptive.license_key'])

        # video_info = extra_info.properties.copy()
        media_source.label = extra_info.label
        media_source.label2 = extra_info.label2
        media_source.plot = extra_info.info['plot'] if 'plot' in extra_info.info else ''
        media_source.art = extra_info.art

        query_string = urllib.parse.urlencode({'media_source' : json.dumps(media_source, default=serialize_namespace)})
        full_url = base_url + query_string
        intent = Intent(main_activity.getApplicationContext(), PlayerActivity)
        intent.setData(Uri.parse(full_url))
        main_activity.startActivity(intent)

    def isPlaying(self):
        return True

def getCondVisibility(condition):
    if condition.startswith('System.'):
        if condition.startswith('System.HasAddon'):
            pattern = r"System\.HasAddon\(['\"]([^'\"]+)['\"]\)"
            match = re.search(pattern, condition)
            if match:
                print(match)
                addon_name = match.group(1)
                print(type(addon_name), type('plugin.video.xbmctorrent'), repr(addon_name), repr('plugin.video.xbmctorrent'), addon_name == 'plugin.video.xbmctorrent')
                if addon_name == 'plugin.video.xbmctorrent':
                    print(True)
                    return True
            return False
    else:
        print(getCondVisibility, condition)
        parts = condition.split('.')
        if len(parts) < 2:
            return False  # Invalid condition format

        if parts[0] == 'system' and parts[1] == 'platform':
            if len(parts) > 2:
                platform_name = parts[2]
                if platform_name == 'android': return True
                if platform_name == 'linux': return True
                else:
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