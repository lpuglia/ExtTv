import sys

# Dummy constants for sorting methods
SORT_METHOD_DATE = 1
SORT_METHOD_TITLE_IGNORE_THE = 2

def addDirectoryItems(handle, dirItems):
    print(f"Called addDirectoryItems with handle: {handle} and dirItems: {dirItems}")

def setContent(handle, content):
    print(f"Called setContent with handle: {handle} and content: {content}")

def addSortMethod(handle, sortMethod):
    print(f"Called addSortMethod with handle: {handle} and sortMethod: {sortMethod}")

def setPluginCategory(handle, category):
    print(f"Called setPluginCategory with handle: {handle} and category: {category}")

def endOfDirectory(handle, succeeded=True, updateListing=False, cacheToDisc=True):
    print(f"Called endOfDirectory with handle: {handle}, succeeded: {succeeded}, updateListing: {updateListing}, cacheToDisc: {cacheToDisc}")
