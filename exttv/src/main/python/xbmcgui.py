class MockGUI:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(MockGUI, cls).__new__(cls)
            cls._instance.logs = []
            cls._instance.widgets = []
        return cls._instance

    def log(self, message):
        # print(message)  # For immediate feedback, optional
        self.logs.append(message)

    def register_widget(self, widget):
        self.widgets.append(widget)

    def get_logs(self):
        return self.logs

    def get_widgets(self):
        return self.widgets

class DialogProgressBG:
    def __init__(self):
        self.mock_gui = MockGUI()
        self.heading = ''
        self.message = ''
        self.status = ''
        self.mock_gui.register_widget(self)

    def create(self, heading, message):
        self.heading = heading
        self.message = message
        self.mock_gui.log(f"DialogProgressBG - Create - Heading: {heading}, Message: {message}")

    def update(self, status, message=""):
        self.status = status
        self.message = message
        self.mock_gui.log(f"DialogProgressBG - Update - Status: {status}, Message: {message}")

class Dialog:
    def __init__(self):
        self.mock_gui = MockGUI()
        self.mock_gui.register_widget(self)
        self.heading = ''
        self.lines = []

    def ok(self, heading, line1, line2='', line3=''):
        self.heading = heading
        self.lines = [line1, line2, line3]
        self.mock_gui.log(f"Dialog - OK - Heading: {heading}, Line1: {line1}, Line2: {line2}, Line3: {line3}")

    def yesno(self, heading, line1, line2='', line3='', nolabel='No', yeslabel='Yes', autoclose=False):
        self.heading = heading
        self.lines = [line1, line2, line3]
        self.mock_gui.log(f"Dialog - Yes/No - Heading: {heading}, Line1: {line1}, Line2: {line2}, Line3: {line3}, NoLabel: {nolabel}, YesLabel: {yeslabel}")
        return True

    def yesnocustom(self, heading, line1, line2='', line3='', nolabel='No', yeslabel='Yes', customlabel='Custom'):
        self.heading = heading
        self.lines = [line1, line2, line3]
        self.mock_gui.log(f"Dialog - Yes/No/Custom - Heading: {heading}, Line1: {line1}, Line2: {line2}, Line3: {line3}, NoLabel: {nolabel}, YesLabel: {yeslabel}, CustomLabel: {customlabel}")
        return 'custom'

class ListItem:
    def __init__(self, label='', label2=''):
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