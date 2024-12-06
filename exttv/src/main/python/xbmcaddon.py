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
        settings_dict = {}  # Dictionary to store id and default values

        # Iterate over all <setting> elements in the XML tree
        for setting in root.iter('setting'):
            setting_id = setting.get('id')
            setting_default = setting.get('default')
            setting_text = setting.text

            if setting_text is None:
                settings_dict[setting_id] = setting_default
            else:
                settings_dict[setting_id] = setting_text

            # # hack
            # if setting_id == 'default_action':
            #     settings_dict['default_action'] = "2"

        return settings_dict

    except ET.ParseError as e:
        print(f"Error parsing XML: {e}")
        return None
    except FileNotFoundError:
        print(f"Error: File '{xml_file}' not found.")
        return None

def parse_settings_from_xml_backup(xml_file):
    try:
        tree = ET.parse(xml_file)  # Parse the XML file
        root = tree.getroot()  # Get the root element
        settings_dict = {}  # Dictionary to store id and default values

        # Iterate over all <setting> elements in the XML tree
        for setting in root.iter('setting'):
            setting_id = setting.get('id')
            setting_default = setting.get('default')

            # If 'default' is an attribute, use it
            if setting_default is None:
                default_element = setting.find('default')
                if default_element is not None:
                    setting_default = default_element.text

            # Add to dictionary if id and default value are available
            if setting_id is not None and setting_default is not None:
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
    current_msgid = None
    current_msgstr = None
    
    with open(file_path, 'r', encoding='utf-8') as file:
        for line in file:
            line = line.strip()
            
            if line.startswith('msgctxt'):
                current_msgctxt = line[10:-1]  # Remove 'msgctxt #' and trailing quote
            elif line.startswith('msgid'):
                current_msgid = line[7:-1]  # Remove 'msgid ' and trailing quote
            elif line.startswith('msgstr'):
                current_msgstr = line[8:-1]  # Remove 'msgstr ' and trailing quote
                if current_msgstr is not None:
                    messages[current_msgctxt] = current_msgid #(current_msgid, current_msgstr)
                    current_msgctxt = None
                    current_msgid = None
                    current_msgstr = None
    return messages

class Addon():

    def __init__(self, id=""):
        self.addon = {}
        if id in ['inputstream.adaptive']:
            self.addon['id'] = 'inputstream.adaptive'
            self.addon['name'] = 'InputStream Adaptive'
            self.addon['version'] = '21.4.4'
            self.addon['author'] = ''
            self.addon['summary'] = ''
            self.addon['description'] = ''
            self.addon['type'] = ''
            self.addon['icon'] = ''
            self.addon['fanart'] = ''
            return
        if id=='':
            id = plugin.plugin_name
        
        po_file_path = os.path.join(utils.full_addons_path(), f'{id}/resources/language/resource.language.en_gb/strings.po')
        # settings_xml_path = os.path.join(utils.full_addons_path(), f'../userdata/addon_data/{id}/settings.xml')
        settings_xml_path = os.path.join(utils.full_addons_path(), f'{id}/resources/settings.xml')
        addon_xml_path = os.path.join(utils.full_addons_path(), f'{id}/addon.xml')

        if os.path.exists(po_file_path):
            self.localizedStrings = parse_po_file(po_file_path)
        else:
            print(f"Warning: PO file '{po_file_path}' not found.")

        if os.path.exists(settings_xml_path):
            self.settings = parse_settings_from_xml_backup(settings_xml_path)
        else:
            print(f"Warning: Settings XML file '{settings_xml_path}' not found.")

        if os.path.exists(addon_xml_path):
            self.addon = parse_addon_xml(addon_xml_path)
        else:
            print(f"Warning: Addon XML file '{addon_xml_path}' not found.")

        self.addon['path'] = os.path.join(utils.full_addons_path(), id)
        self.addon['Profile'] = os.path.join(utils.full_addondata_path(), id)

        self.addon['Path'] = self.addon['path']
        self.addon['profile'] = self.addon['Profile']

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
    