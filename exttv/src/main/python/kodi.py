import os
import sys
import utils
from time import sleep

from xbmc import KodiNavigationStack
from xbmcplugin import PluginRecorder
from xbmcgui import Dialog

plugin = PluginRecorder()
nav_stack = KodiNavigationStack()

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

kod_folder_name = 'plugin.video.kod'
url = 'https://github.com/kodiondemand/addon/archive/refs/heads/master.zip'
utils.download_and_extract_plugin(url, kod_folder_name, False)

sys.path.append(os.path.join(utils.full_addons_path(), kod_folder_name))

sys.argv = [f'plugin://{kod_folder_name}/', '3', "?"]
sys.argv[2] = '?ewogICAgImFjdGlvbiI6ICJmaW5kdmlkZW9zIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImZvcmNldGh1bWIiOiB0cnVlLAogICAgImZ1bGx0aXRsZSI6ICJMYTciLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAibm9fcmV0dXJuIjogdHJ1ZSwKICAgICJ0aHVtYm5haWwiOiAiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2tvZGlvbmRlbWFuZC9tZWRpYS9tYXN0ZXIvbGl2ZS9sYTcucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1MYTdbL0JdIiwKICAgICJ1cmwiOiAiaHR0cHM6Ly93d3cubGE3Lml0L2RpcmV0dGUtdHYiCn0%3D'

nav_stack._path_stack.append(sys.argv[0]+sys.argv[2])
import default
