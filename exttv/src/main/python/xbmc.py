import os
import re
import time
import json
import utils
import platform
import urllib.parse, urllib.request
from datetime import datetime
from types import SimpleNamespace

def serialize_namespace(obj):
    if isinstance(obj, SimpleNamespace):
        return obj.__dict__
    raise TypeError(f"Object of type {obj.__class__.__name__} is not JSON serializable")

try:
    from java import jclass
    main_activity = jclass("com.android.exttv.view.MainActivity").getInstance()
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

localized_strings = {}
try:
    with urllib.request.urlopen('https://raw.githubusercontent.com/xbmc/xbmc/eaca754dc37ea9796bd2d303c7d5f43089785e4b/addons/resource.language.en_gb/resources/strings.po') as response:
        content = response.read().decode('utf-8')
        pattern = re.compile(r'msgctxt\s+"#(\d+)"\nmsgid\s+"(.*?)"\nmsgstr\s+"(.*?)"', re.DOTALL)
        matches = pattern.findall(content)
        for match in matches:
            msgctxt, msgid, msgstr = match
            localized_strings[int(msgctxt)] = msgstr if msgstr else msgid
except:
    print('Couldn\' get strings.po')

def sleep(milliseconds):
    time.sleep(milliseconds / 1000)

def executebuiltin(command, wait=False):
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

# Parser to handle incoming JSON-RPC requests
def executeJSONRPC(jsonrpc_request):
    try:
        # Parse the JSON-RPC request
        request = json.loads(jsonrpc_request)

        # Extract method and parameters
        method_name = request.get("method")
        params = request.get("params", {})
        method_id = request.get("id", None)
        print(method_name, params, method_id)

        if method_name.startswith('Settings.'):
            response_tmplt = '{{"id":0,"jsonrpc":"2.0","result":{{"value":{}}}}}'
            if method_name.endswith('GetSettingValue'):
                setting = params.get('setting')
                if setting == "lookandfeel.skin":
                    return response_tmplt.format("\"skin.estuary\"")
                elif setting == 'network.usehttpproxy':
                    return response_tmplt.format("false")
                elif setting == 'network.httpproxyserver':
                    return response_tmplt.format("\"\"")
                elif setting == 'network.httpproxyport':
                    return response_tmplt.format("0")
                elif setting == 'network.httpproxyusername':
                    return response_tmplt.format("\"\"")
                elif setting == 'network.httpproxypassword':
                    return response_tmplt.format("\"\"")
                else:
                    raise NotImplementedError(f"Setting {setting} not implemented")
        elif method_name.startswith('Addons.'):
            response_tmplt = '{{"id":0,"jsonrpc":"2.0","result":{{"addon":{}}}}}'
            if method_name.endswith('GetAddonDetails'):
                addon_id = params.get('addonid')
                if addon_id == 'inputstream.adaptive':
                    return response_tmplt.format('{ "addonid": "inputstream.adaptive", "name": "InputStream Adaptive", "version": "21.4.4", "summary": "", "description": "", "path": "", "enabled": true, "dependencies": []  }')
                else:
                    raise NotImplementedError(f"Addon {addon_id} not implemented")
        else:
            raise NotImplementedError(f"Method {method_name} not implemented")
            
    except json.JSONDecodeError:
        raise ValueError("Invalid JSON-RPC request")
    except Exception as e:
        raise ValueError(f"Error executing JSON-RPC request: {e}")
    return response


def getRegion(setting):
    settings = {
            'datelong': 'DDDD, D MMMM YYYY',
            'dateshort': 'DD-MM-YYYY',
            'time': 'H:mm:ss',
            'meridiem': 'AM/PM',
            'tempunit': 'C',
            'speedunit': 'kmh',
            'timezone': 'CET',
            'thousandsseparator': ',',
            'decimalseparator': '.'
        }
    return settings[setting]

def getLocalizedString(string_id):
    return localized_strings.get(string_id, "")

def getSupportedMedia(media_type):
    # Dictionary of supported file types for different media types
    media_types = {
        'video': '.mp4|.avi|.mov|.mkv|.flv',
        'music': '.mp3|.wav|.flac|.aac|.ogg',
        'picture': '.jpg|.png|.bmp|.gif|.tiff'
    }
    
    # Get the supported file types for the given media type
    return media_types.get(media_type, '')

class Logger:
    def __init__(self):
        self.log_file = os.path.join(utils.full_home_path(), 'kodi.log')
        # Ensure the log directory exists
        os.makedirs(os.path.dirname(self.log_file), exist_ok=True)
        if not os.path.exists(self.log_file):
            with open(self.log_file, 'w'): pass
    
    def __call__(self, msg, level=LOGINFO):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(self.log_file, 'a') as file:
            log_entry = f"{timestamp} - {level}: {msg}\n"
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
        base_url = "exttv_player://app/?"
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
        main_activity.executeStartActivity(f'StartAndroidActivity("", "android.intent.action.VIEW", "", "{full_url}")')

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