from typing import List
from types import SimpleNamespace

try:

    from java import dynamic_proxy, jclass
    from java.lang import Runnable
    from android.app import AlertDialog, ProgressDialog
    from android.content import DialogInterface
    import threading
    main_activity = jclass("com.android.exttv.MainActivity").getInstance()

    def run_on_ui_thread(func):
        def wrapper():
            class R(dynamic_proxy(Runnable)):
                def run(self):
                    func()

            main_activity.runOnUiThread(R())
        return wrapper

except ImportError as e:
     print("Could not import MainActivity", e)
     main_activity = None

DLG_YESNO_NO_BTN = "No"
DLG_YESNO_YES_BTN = "Yes"
DLG_YESNO_CUSTOM_BTN = "Custom"

def getCurrentWindowId():
    return 1


class MockGUI:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(MockGUI, cls).__new__(cls)
            cls._instance.logs = []
            cls._instance.widgets = []
        return cls._instance

    def log(self, message):
        self.logs.append(message)

    def register_widget(self, widget):
        self.widgets.append(widget)

    def get_logs(self):
        return self.logs

    def get_widgets(self):
        return self.widgets

class DialogProgressBG:
    def __init__(self):
        if main_activity is None: return
        self.progress_percent = 0
        state_done = threading.Event()
        @run_on_ui_thread
        def instantiate_dialog():
            self.dialog = ProgressDialog(main_activity)
            state_done.set()
        instantiate_dialog()
        state_done.wait()

    def create(self, heading, message=''):
        if main_activity is None:
            MockGUI().log(f"DialogProgressBG - create - Heading: {heading}, Message: {message}")
            return

        @run_on_ui_thread
        def show_dialog():
            self.dialog.setTitle(heading)
            self.dialog.setMessage(message)
            self.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            self.dialog.setMax(100)
            self.dialog.setProgress(self.progress_percent)
            self.dialog.setCanceledOnTouchOutside(True)
            self.dialog.setCancelable(True)

            class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                def onCancel(self, dialog):
                    self.is_canceled = True
            self.dialog.setOnCancelListener(OnCancelListener())

            self.dialog.show()

        show_dialog()

    def update(self, percent=None, heading=None, message=None):
        if main_activity is None:
            MockGUI().log(f"DialogProgressBG - update - Percent: {percent}, Heading: {heading}, Message: {message}")
            return

        if self.dialog is None or not self.dialog.isShowing():
            return

        @run_on_ui_thread
        def update_progress():
            if self.dialog:
                if percent is not None:
                    self.progress_percent = percent
                    self.dialog.setProgress(self.progress_percent)

                if heading is not None:
                    self.dialog.setTitle(heading)

                if message is not None:
                    self.dialog.setMessage(message)

        update_progress()

    def close(self):
        if main_activity is None:
            MockGUI().log("DialogProgressBG - close")
            return

        if self.dialog is not None and self.dialog.isShowing():
            self.dialog.dismiss()
            self.dialog = None
            self.is_canceled = False

    def isFinished(self):
        if main_activity is None:
            MockGUI().log("DialogProgressBG - isFinished")
            return False

        return self.dialog is None or not self.dialog.isShowing()

class DialogProgress:
    def __init__(self):
        self.progress_percent = 0
        self.is_canceled = False
        if main_activity is None: return
        state_done = threading.Event()
        @run_on_ui_thread
        def instantiate_dialog():
            self.dialog = ProgressDialog(main_activity)
            state_done.set()
        instantiate_dialog()
        state_done.wait()

    def create(self, heading, message=''):
        if main_activity is None:
            MockGUI().log(f"DialogProgress - create - Heading: {heading}, Message: {message}")
            return

        @run_on_ui_thread
        def show_dialog():
            self.dialog.setTitle(heading)
            self.dialog.setMessage(message)
            self.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            self.dialog.setMax(100)
            self.dialog.setProgress(self.progress_percent)
            self.dialog.setCanceledOnTouchOutside(True)
            self.dialog.setCancelable(True)  # Allow cancelation

            class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                def onCancel(self, dialog):
                    self.is_canceled = True
            self.dialog.setOnCancelListener(OnCancelListener())

            self.dialog.show()
        show_dialog()

    def update(self, percent, message=None):
        if main_activity is None:
            MockGUI().log(f"DialogProgress - update - Percent: {percent}, Message: {message}")
            return

        if self.dialog is None or not self.dialog.isShowing():
            return

        state_done = threading.Event()
        @run_on_ui_thread
        def update_progress():
            if self.dialog is not None and self.dialog.isShowing():
                self.progress_percent = percent
                self.dialog.setProgress(self.progress_percent)

                if message is not None:
                    self.dialog.setMessage(message)
            state_done.set()

        update_progress()
        state_done.wait()

    def close(self):
        if main_activity is None:
            MockGUI().log("DialogProgress - close")
            return

        if self.dialog is not None and self.dialog.isShowing():
            self.dialog.dismiss()
            self.dialog = None
            self.is_canceled = False

    def iscanceled(self):
        return self.is_canceled

class ListItem:
    def __init__(self, label='', label2='', path='', offscreen=False):
        self.mock_gui = MockGUI()
        self.label = label
        self.label2 = label2
        self.info = {}
        self.art = {}
        self.context_menu_items = []
        self.properties = {}
        self.path = ''
        self.mimetype = ''
        self.subtitles = []
        self.path = path
        self.offscreen = offscreen
        self.mock_gui.register_widget(self)

    def setLabel(self, label):
        self.label = label
        self.mock_gui.log(f"ListItem - SetLabel - Label: {label}")

    def setLabel2(self, label2):
        self.label2 = label2
        self.mock_gui.log(f"ListItem - SetLabel2 - Label2: {label2}")

    def setInfo(self, type, infoLabels):
        self.info[type] = infoLabels
        self.mock_gui.log(f"ListItem - SetInfo - Type: {type}, InfoLabels: {infoLabels}")

    def setArt(self, art):
        self.art.update(art)
        self.mock_gui.log(f"ListItem - SetArt - Art: {art}")

    def addContextMenuItems(self, items, replaceItems=False):
        if replaceItems:
            self.context_menu_items = items
        else:
            self.context_menu_items.extend(items)
        self.mock_gui.log(f"ListItem - AddContextMenuItems - Items: {items}, ReplaceItems: {replaceItems}")

    def setProperty(self, key, value):
        self.properties[key] = value
        self.mock_gui.log(f"ListItem - SetProperty - Key: {key}, Value: {value}")

    def getProperty(self, key):
        value = self.properties.get(key, '')
        self.mock_gui.log(f"ListItem - GetProperty - Key: {key}, Value: {value}")
        return value

    def setPath(self, path):
        self.path = path
        self.mock_gui.log(f"ListItem - SetPath - Path: {path}")

    def setMimeType(self, mimetype):
        self.mimetype = mimetype
        self.mock_gui.log(f"ListItem - SetMimeType - MimeType: {mimetype}")

    def setSubtitles(self, subtitleFiles):
        self.subtitles.extend(subtitleFiles)
        self.mock_gui.log(f"ListItem - SetSubtitles - Subtitles: {subtitleFiles}")

    def __str__(self):
        return f"ListItem: label={self.label}, label2={self.label2}, path={self.path}, offscreen={self.offscreen}, info={self.info}, art={self.art}, context_menu_items={self.context_menu_items}, properties={self.properties}, mimetype={self.mimetype}, subtitles={self.subtitles}"


class ControlTextBox:
    def __init__(self, x: int, y: int, width: int, height: int, font: str = None, textColor: str = None):
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.font = font
        self.textColor = textColor

    def setText(self, text: str) -> None:
        pass

    def getText(self) -> str:
        pass

    def reset(self) -> None:
        pass

    def scroll(self, id: int) -> None:
        pass

    def autoScroll(self, delay: int, time: int, repeat: int) -> None:
        pass

class WindowXMLDialog:

    def __init__(self, xmlFilename: str, scriptPath: str, defaultSkin: str = 'Default', defaultRes: str = '720p'):

        def ControlList():
            cl = SimpleNamespace()

            def __init__(
                x: int = 0, y: int = 0, width: int = 100, height: int = 100, font: str = None, textColor: str = None, buttonTexture: str = None, buttonFocusTexture: str = None, selectedColor: str = None, _imageWidth: int = 10, _imageHeight: int = 10, _itemTextXOffset: int = 10, _itemTextYOffset: int = 2, _itemHeight: int = 27, _space: int = 2, _alignmentY: int = 4
            ):
                cl.x = x
                cl.y = y
                cl.width = width
                cl.height = height
                cl.font = font
                cl.textColor = textColor
                cl.buttonTexture = buttonTexture
                cl.buttonFocusTexture = buttonFocusTexture
                cl.selectedColor = selectedColor
                cl._imageWidth = _imageWidth
                cl._imageHeight = _imageHeight
                cl._itemTextXOffset = _itemTextXOffset
                cl._itemTextYOffset = _itemTextYOffset
                cl._itemHeight = _itemHeight
                cl._space = _space
                cl._alignmentY = _alignmentY
                cl.items = []
                cl.selectedPosition = -1

            def addItem(item, sendMessage: bool = True) -> None:
                cl.items.append(item)

            def addItems(items) -> None:
                cl.items.extend(items)

            def selectItem(item: int) -> None:
                cl.selectedPosition = item

            def removeItem(index: int) -> None:
                if index >= 0 and index < len(cl.items):
                    del cl.items[index]

            def reset() -> None:
                pass

            def getSelectedPosition() -> int:
                return cl.selectedPosition

            def getSelectedItem() -> 'ListItem':
                return cl.items[cl.selectedPosition]

            def setImageDimensions(imageWidth: int, imageHeight: int) -> None:
                pass

            def setSpace(space: int) -> None:
                pass

            def setPageControlVisible(visible: bool) -> None:
                pass

            def size() -> int:
                return len(cl.items)

            def getItemHeight() -> int:
                pass

            def getSpace() -> int:
                pass

            def getListItem(index: int) -> 'ListItem':
                return cl.items[index]

            def setStaticContent(items: List['ListItem']) -> None:
                pass

            # Assign functions to the namespace object
            cl.__init__ = __init__
            cl.addItem = addItem
            cl.addItems = addItems
            cl.selectItem = selectItem
            cl.removeItem = removeItem
            cl.reset = reset
            cl.getSelectedPosition = getSelectedPosition
            cl.getSelectedItem = getSelectedItem
            cl.setImageDimensions = setImageDimensions
            cl.setSpace = setSpace
            cl.setPageControlVisible = setPageControlVisible
            cl.size = size
            cl.getItemHeight = getItemHeight
            cl.getSpace = getSpace
            cl.getListItem = getListItem
            cl.setStaticContent = setStaticContent

            # Initialize default values
            cl.__init__()

            return cl


        def ControlLabel():
            cl = SimpleNamespace()
            
            def __init__(x: int = 0, y: int = 0, width: int = 0, height: int = 0, label: str = '', font: str = None, textColor: str = None, disabledColor: str = None, alignment: int = 0, hasPath: bool = False, angle: int = 0):
                cl.x = x
                cl.y = y
                cl.width = width
                cl.height = height
                cl.label = label
                cl.font = font
                cl.textColor = textColor
                cl.disabledColor = disabledColor
                cl.alignment = alignment
                cl.hasPath = hasPath
                cl.angle = angle

            def getLabel() -> str:
                return cl.label

            def setLabel(label: str = '', font: str = None, textColor: str = None, disabledColor: str = None, shadowColor: str = None, focusedColor: str = None, label2: str = ''):
                cl.label = label
                cl.font = font
                cl.textColor = textColor
                cl.disabledColor = disabledColor
                cl.shadowColor = shadowColor
                cl.focusedColor = focusedColor
                cl.label2 = label2

            cl.__init__ = __init__
            cl.getLabel = getLabel
            cl.setLabel = setLabel

            # Initialize default values
            cl.__init__()

            return cl

        def ControlButton():
            cb = SimpleNamespace()

            def __init__(
                x: int = 0, y: int = 0, width: int = 100, height: int = 50, label: str = '', focusTexture: str = None, noFocusTexture: str = None, textOffsetX: int = 10, textOffsetY: int = 2, alignment: int = 4, font: str = None, textColor: str = None, disabledColor: str = None, angle: int = 0, shadowColor: str = None, focusedColor: str = None
            ):
                cb.x = x
                cb.y = y
                cb.width = width
                cb.height = height
                cb.label = label
                cb.focusTexture = focusTexture
                cb.noFocusTexture = noFocusTexture
                cb.textOffsetX = textOffsetX
                cb.textOffsetY = textOffsetY
                cb.alignment = alignment
                cb.font = font
                cb.textColor = textColor
                cb.disabledColor = disabledColor
                cb.angle = angle
                cb.shadowColor = shadowColor
                cb.focusedColor = focusedColor

            def setLabel(
                label: str = '', font: str = None, textColor: str = None, disabledColor: str = None, shadowColor: str = None, focusedColor: str = None, label2: str = ''
            ) -> None:
                cb.label = label
                cb.font = font
                cb.textColor = textColor
                cb.disabledColor = disabledColor
                cb.shadowColor = shadowColor
                cb.focusedColor = focusedColor
                cb.label2 = label2

            def setDisabledColor(disabledColor: str) -> None:
                cb.disabledColor = disabledColor

            def getLabel() -> str:
                return cb.label

            def getLabel2() -> str:
                return getattr(cb, 'label2', '')

            # Assign functions to the namespace object
            cb.__init__ = __init__
            cb.setLabel = setLabel
            cb.setDisabledColor = setDisabledColor
            cb.getLabel = getLabel
            cb.getLabel2 = getLabel2

            # Initialize with default values
            cb.__init__()

            return cb


        self.xmlField = {}
        if xmlFilename == "DialogSelect.xml":
            self.xmlField[1] = ControlLabel()	     # Heading label
            self.xmlField[2] = ControlLabel()	     # Number of items in the list
            self.xmlField[3] = ControlList()	 # List of available options
            self.xmlField[5] = ControlButton()	 # OK (on multiple selection) or Manual/Get More... button
            self.xmlField[6] = ControlList()	 # List of available add-ons
            self.xmlField[7] = ControlButton()	 # Cancel button
            self.xmlField[8] = ControlButton()	 # Add/Get More... button
            self.xmlField[11] = ControlList() # List with game video thumbnails
            self.xmlField[12] = ControlTextBox	 # Description of the currently-selected video filter
        pass

    def getControl(self, controlId: int):
        return self.xmlField.get(controlId, None)

    def doModal(self):
        if main_activity is None:
            self.mock_gui = MockGUI()
            self.mock_gui.log(f"WindowXMLDialog: doModal")
        else:
            self.onInit()
            # return Dialog().select(self.xmlField[1].getLabel(), [i.label for i in self.xmlField[6].items])
        
            state_done = threading.Event()
            selected_position = [-1]

            @run_on_ui_thread
            def show_dialog():
                builder = AlertDialog.Builder(main_activity)
                builder.setTitle(self.xmlField[1].getLabel())
                
                prova = self
                class OnClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                    def onClick(self, dialog, which):
                        prova.xmlField[6].selectItem(which)
                        prova.selection = prova.itemlist[prova.SERVERS.getSelectedPosition()]
                        prova.onClick(6)
                        # selected_position[0] = which
                        state_done.set()

                class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                    def onCancel(self, dialog):
                        selected_position[0] = -1  # Dialog cancelled
                        state_done.set()

                builder.setItems([i.label for i in self.xmlField[6].items], OnClickListener());
                builder.setOnCancelListener(OnCancelListener())
                dialog = builder.create()
                dialog.show()

            show_dialog()

            state_done.wait()
            print(self.selection)
    
    def close(self): # dummy
        pass


    @staticmethod
    def setFocus(servers):
        print("setFocus!", servers)
        pass

class Dialog:

    def __init__(self):
        self.mock_gui = MockGUI()
        self.mock_gui.register_widget(self)
        self.heading = ''
        self.lines = []

    def ok(self, heading, message):
        if main_activity is None:
            self.mock_gui.log(f"Dialog - OK - Heading: {heading}, Message: {message}")
            return True
        else:
            state_done = threading.Event()
            to_return = [False]
            
            @run_on_ui_thread
            def show_dialog():
                builder = AlertDialog.Builder(main_activity)
                builder.setTitle(heading)
                builder.setMessage(message)

                class OnClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                    def onClick(self, dialog, which):
                        to_return[0] = True
                        state_done.set()

                class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                    def onCancel(self, dialog):
                        to_return[0] = False
                        state_done.set()

                builder.setPositiveButton("OK", OnClickListener())
                builder.setOnCancelListener(OnCancelListener())
                dialog = builder.create()
                dialog.show()

            show_dialog()

            state_done.wait()
            return to_return[0]

    def yesno(self, heading, message, nolabel='No', yeslabel='Yes', autoclose=False): # autoclose not implemented
        if main_activity is None:
            self.mock_gui.log(f"Dialog - YesNo - Heading: {heading}, Message: {message}")
            return False
        else:
            state_done = threading.Event()
            to_return = [False]
            
            @run_on_ui_thread
            def show_dialog():
                builder = AlertDialog.Builder(main_activity)
                builder.setTitle(heading)
                builder.setMessage(message)

                class YesClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                    def onClick(self, dialog, which):
                        to_return[0] = True
                        if autoclose:
                            dialog.dismiss()
                        state_done.set()

                class NoClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                    def onClick(self, dialog, which):
                        to_return[0] = False
                        if autoclose:
                            dialog.dismiss()
                        state_done.set()

                class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                    def onCancel(self, dialog):
                        to_return[0] = False
                        state_done.set()

                builder.setPositiveButton(yeslabel, YesClickListener())
                builder.setNegativeButton(nolabel, NoClickListener())
                builder.setOnCancelListener(OnCancelListener())
                dialog = builder.create()
                dialog.show()

            show_dialog()

            state_done.wait()
            return to_return[0]

    def yesnocustom(self, heading, message, customlabel=None, nolabel='No', yeslabel='Yes', autoclose=False): # autoclose not implemented
        if main_activity is None:
            self.mock_gui.log(f"Dialog - YesNoCustom - Heading: {heading}, Message: {message}, Custom Label: {customlabel}, No Label: {nolabel}, Yes Label: {yeslabel}")
            return -1
        else:
            state_done = threading.Event()
            to_return = [-1]

            @run_on_ui_thread
            def show_dialog():
                builder = AlertDialog.Builder(main_activity)
                builder.setTitle(heading)
                builder.setMessage(message)

                class OnClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                    def onClick(self, dialog, which):
                        if which == DialogInterface.BUTTON_NEGATIVE:
                            to_return[0] = 0  # No button clicked
                        elif which == DialogInterface.BUTTON_POSITIVE:
                            to_return[0] = 1  # Yes button clicked
                        elif which == DialogInterface.BUTTON_NEUTRAL:
                            to_return[0] = 2  # Custom button clicked
                        state_done.set()

                class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                    def onCancel(self, dialog):
                        to_return[0] = -1  # Dialog cancelled
                        state_done.set()

                builder.setPositiveButton(yeslabel, OnClickListener())
                builder.setNegativeButton(nolabel, OnClickListener())
                if customlabel:
                    builder.setNeutralButton(customlabel, OnClickListener())
                builder.setOnCancelListener(OnCancelListener())

                dialog = builder.create()
                dialog.setCancelable(True)
                dialog.show()

            show_dialog()

            state_done.wait()
            return to_return[0]
    
    def select(self, heading, items, autoclose=None, preselect=None, useDetails=False):
        if main_activity is None:
            self.mock_gui.log(f"Dialog - Select - Heading: {heading}, Items: {items}, Autoclose: {autoclose}, "
                              f"Preselect: {preselect}, UseDetails: {useDetails}")
            return 0  # Mock return value when using MockGUI
        
        state_done = threading.Event()
        selected_position = [-1]

        @run_on_ui_thread
        def show_dialog():
            builder = AlertDialog.Builder(main_activity)
            builder.setTitle(heading)
            
            class OnClickListener(dynamic_proxy(DialogInterface.OnClickListener)):
                def onClick(self, dialog, which):
                    selected_position[0] = which
                    state_done.set()

            class OnCancelListener(dynamic_proxy(DialogInterface.OnCancelListener)):
                def onCancel(self, dialog):
                    selected_position[0] = -1  # Dialog cancelled
                    state_done.set()

            builder.setItems(items, OnClickListener());
            builder.setOnCancelListener(OnCancelListener())
            dialog = builder.create()
            dialog.show()

        show_dialog()

        state_done.wait()
        return selected_position[0]



class Window():

    def __init__(self, existingWindowId=None):
        pass

    def show(self):
        pass

    def setFocus(self, control):
        pass

    def setFocusId(self, controlId):
        pass

    def getFocus(self):
        pass

    def getFocusId(self):
        pass

    def removeControl(self, control):
        pass

    def removeControls(self, controlList):
        pass

    def getHeight(self):
        pass

    def getWidth(self):
        pass

    def setProperty(self, key, value):
        pass

    def getProperty(self, key):
        pass

    def clearProperty(self, key):
        pass

    def clearProperties(self):
        pass

    def close(self):
        pass

    def doModal(self):
        pass

    def addControl(self, control):
        pass

    def addControls(self, controlList):
        pass

    def getControl(self, controlId):
        pass

# Main function to test DialogProgressBG
def main():
    # Create DialogProgressBG instance
    pDialog = DialogProgressBG()

    # Create and show the dialog
    pDialog.create('Movie Trailers', 'Downloading Monsters Inc...')

    import time
    # Simulate updating the progress and messages
    for i in range(5):
        percent = (i + 1) * 20
        print(percent)
        pDialog.update(percent, message=f'Downloading part {i + 1}...')
        time.sleep(2)

    # Close the dialog
    pDialog.close()

    # Check if finished
    print(pDialog.isFinished())
