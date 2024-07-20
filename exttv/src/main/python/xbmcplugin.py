import sys
from typing import List, Tuple
import xbmcgui

# Dummy constants for sorting methods
SORT_METHOD_DATE = 1
SORT_METHOD_TITLE_IGNORE_THE = 2

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

# Replace the original functions with wrapped versions that record calls
def addDirectoryItems(handle, items: List[Tuple[str, xbmcgui.ListItem, bool]], totalItems: int = 0):
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
