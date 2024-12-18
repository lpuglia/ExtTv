from typing import List, Tuple
import xbmcgui
from xbmc import Player
import utils

# Dummy constants for sorting methods
SORT_METHOD_NONE = 0
SORT_METHOD_LABEL = 1
SORT_METHOD_LABEL_IGNORE_THE = 2
SORT_METHOD_DATE = 3
SORT_METHOD_SIZE = 4
SORT_METHOD_FILE = 5
SORT_METHOD_DRIVE_TYPE = 6
SORT_METHOD_TRACKNUM = 7
SORT_METHOD_DURATION = 8
SORT_METHOD_TITLE = 9
SORT_METHOD_TITLE_IGNORE_THE = 10
SORT_METHOD_ARTIST = 11
SORT_METHOD_ARTIST_IGNORE_THE = 12
SORT_METHOD_ALBUM = 13
SORT_METHOD_ALBUM_IGNORE_THE = 14
SORT_METHOD_GENRE = 15
SORT_METHOD_YEAR = 16
SORT_METHOD_VIDEO_RATING = 17
SORT_METHOD_PROGRAM_COUNT = 18
SORT_METHOD_PLAYLIST_ORDER = 19
SORT_METHOD_EPISODE = 20
SORT_METHOD_VIDEO_TITLE = 21
SORT_METHOD_VIDEO_SORT_TITLE = 22
SORT_METHOD_VIDEO_SORT_TITLE_IGNORE_THE = 23
SORT_METHOD_PRODUCTIONCODE = 24
SORT_METHOD_SONG_RATING = 25
SORT_METHOD_MPAA_RATING = 26
SORT_METHOD_VIDEO_RUNTIME = 27
SORT_METHOD_STUDIO = 28
SORT_METHOD_STUDIO_IGNORE_THE = 29
SORT_METHOD_UNSORTED = 30
SORT_METHOD_BITRATE = 31
SORT_METHOD_LISTENERS = 32
SORT_METHOD_COUNTRY = 33
SORT_METHOD_DATEADDED = 34
SORT_METHOD_FULLPATH = 35
SORT_METHOD_LABEL_IGNORE_FOLDERS = 36
SORT_METHOD_LASTPLAYED = 37
SORT_METHOD_PLAYCOUNT = 38
SORT_METHOD_CHANNEL = 39
SORT_METHOD_DATE_TAKEN = 40
SORT_METHOD_VIDEO_USER_RATING = 41
SORT_METHOD_SONG_USER_RATING = 42


class PluginRecorder:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.closed = True
            cls._instance._to_return_items = []
            
        return cls._instance

    def record_call(self, func_name, to_record):
        if 'directory_item' in func_name:
            if self.closed: # if we're starting a new directory, reset the items list
                self._to_return_items = []
                self.closed = False
            if func_name == 'directory_items':
                self._to_return_items.extend(to_record)
            elif func_name == 'directory_item':
                self._to_return_items.append(to_record)
        elif func_name == 'end_of_directory':
            self.closed = True

# Create a singleton instance of PluginRecorder
plugin_recorder = PluginRecorder()

def addDirectoryItem(handle: int, url: str, listitem: xbmcgui.ListItem, isFolder: bool = False, totalItems: int = 0):
    utils.parent_uri_map[url] = utils.last_uri
    plugin_recorder.record_call('directory_item', (url, listitem, isFolder))
    # print(f"Called addDirectoryItems with handle: {handle} and dirItems: {dirItems}")

def addDirectoryItems(handle, items: List[Tuple[str, xbmcgui.ListItem, bool]], totalItems: int = 0):
    for item in items:
        utils.parent_uri_map[item[0]] = utils.last_uri
    plugin_recorder.record_call('directory_items', items)
    # print(f"Called addDirectoryItems with handle: {handle} and dirItems: {dirItems}")

def setContent(handle, content):
    plugin_recorder.record_call('content', content)
    # print(f"Called setContent with handle: {handle} and content: {content}")

def addSortMethod(handle, sortMethod):
    plugin_recorder.record_call('sort_method', sortMethod)
    # print(f"Called addSortMethod with handle: {handle} and sortMethod: {sortMethod}")

def setPluginCategory(handle, category):
    plugin_recorder.record_call('plugin_category', category)
    # print(f"Called setPluginCategory with handle: {handle} and category: {category}")

def endOfDirectory(handle, succeeded=True, updateListing=False, cacheToDisc=True):
    plugin_recorder.record_call('end_of_directory', [succeeded, updateListing, cacheToDisc])
    # print(f"Called endOfDirectory with handle: {handle}, succeeded: {succeeded}, updateListing: {updateListing}, cacheToDisc: {cacheToDisc}")

def setResolvedUrl(handle, succeeded : bool, listitem: xbmcgui.ListItem):
    Player().play(listitem)
    plugin_recorder.record_call('resolved_url', [succeeded, listitem])
    # print(f"Called setResolvedUrl with handle: {handle}, succeeded: {succeeded}, url: {url}")