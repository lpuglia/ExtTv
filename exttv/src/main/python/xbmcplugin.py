import sys

# Dummy constants for sorting methods
SORT_METHOD_DATE = 1
SORT_METHOD_TITLE_IGNORE_THE = 2

class PluginRecorder:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.calls = {}
        return cls._instance

    def record_call(self, func_name, to_record):
        self.calls[func_name] = to_record

    def get_calls(self, func_name=None):
        if func_name:
            return self.calls.get(func_name, [])
        return self.calls
    
    def __getitem__(self, key):
        return self.calls[key]

# Create a singleton instance of PluginRecorder
plugin_recorder = PluginRecorder()

# Replace the original functions with wrapped versions that record calls
def addDirectoryItems(handle, dirItems):
    plugin_recorder.record_call('directory_item', dirItems)
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
