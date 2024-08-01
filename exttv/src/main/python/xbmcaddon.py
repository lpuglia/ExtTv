import os
import xml.etree.ElementTree as ET
import utils
from xbmcplugin import PluginRecorder
plugin = PluginRecorder()

def parse_addon_xml(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()

    addon_info = {
        'id': root.attrib.get('id', ''),
        'name': root.attrib.get('name', ''),
        'version': root.attrib.get('version', ''),
        'author': root.attrib.get('provider-name', ''),
        'summary': '',
        'description': '',
        'type': '',
        'icon': '',
        'fanart': ''
    }

    # Iterate over the children of the root element
    for child in root:
        if child.tag == 'extension':
            # Check the point attribute to determine the type
            if 'point' in child.attrib:
                if child.attrib['point'] == 'xbmc.addon.metadata':
                    for metadata in child:
                        if metadata.tag == 'summary' and 'lang' in metadata.attrib and metadata.attrib['lang'] == 'en':
                            addon_info['summary'] = metadata.text
                        elif metadata.tag == 'description' and 'lang' in metadata.attrib and metadata.attrib['lang'] == 'en':
                            addon_info['description'] = metadata.text
                        elif metadata.tag == 'assets':
                            for asset in metadata:
                                if asset.tag == 'icon':
                                    addon_info['icon'] = asset.text
                                elif asset.tag == 'fanart':
                                    addon_info['fanart'] = asset.text
                else:
                    addon_info['type'] = child.attrib['point']

    return addon_info

def parse_settings_from_xml(xml_file):
    try:
        tree = ET.parse(xml_file)  # Parse the XML file
        root = tree.getroot()  # Get the root element
        settings_dict = {}  # Dictionary to store id and default attributes

        # Iterate over <category> elements
        for category in root.findall('category'):
            # Iterate over <setting> elements within each <category>
            for setting in category.findall('setting'):
                setting_id = setting.get('id')
                setting_default = setting.get('default')
                settings_dict[setting_id] = setting_default

        return settings_dict

    except ET.ParseError as e:
        print(f"Error parsing XML: {e}")
        return None
    except FileNotFoundError:
        print(f"Error: File '{xml_file}' not found.")
        return None

def parse_po_file(file_path):

    messages = {}
    current_msgctxt = None
    current_msgstr = None
    
    with open(file_path, 'r', encoding='utf-8') as file:
        for line in file:
            line = line.strip()
            
            if line.startswith('msgctxt'):
                current_msgctxt = line[10:-1]  # Remove 'msgctxt #' and trailing quote
            elif line.startswith('msgstr'):
                current_msgstr = line[8:-1]  # Remove 'msgstr ' and trailing quote
                if current_msgctxt and current_msgstr:
                    messages[current_msgctxt] = current_msgstr
                    current_msgctxt = None
                    current_msgstr = None
    return messages

class Addon():

    def __init__(self, id=""):
        if id=='':
            id = plugin.plugin_name
        self.localizedStrings = parse_po_file(os.path.join(utils.full_addons_path(),'plugin.video.kod/resources/language/resource.language.it_it/strings.po'))
        self.settings = parse_settings_from_xml(os.path.join(utils.full_addons_path(),'plugin.video.kod/resources/settings.xml'))
        self.addon = parse_addon_xml(os.path.join(utils.full_addons_path(),'plugin.video.kod/addon.xml'))

        self.addon['path'] = os.path.join(utils.full_addons_path(),'plugin.video.kod')
        self.addon['Profile'] = os.path.join(utils.full_addondata_path(),'plugin.video.kod')

        self.addon['Path'] = self.addon['path']
        # self.addon['Profile'] = self.addon['profile']

        if not os.path.exists(os.path.join(self.addon['Profile'],'settings_channels')):
            os.makedirs(os.path.join(self.addon['Profile'], 'settings_channels'), exist_ok=True)
        
        self.id = id

    def getLocalizedString(self, id):
        return self.localizedStrings[str(id)]

    def setSetting(self, name, value):
        self.settings[name] = value

    def getSetting(self, name):
        if name == 'show_once': return 'true'
        if name in self.settings:
            return self.settings[name]
        else:
            return None

    def getAddonInfo(self, info):
        return self.addon[info]        
    