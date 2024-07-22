import os
import sys
import utils
from java import jclass
from java.util import Arrays

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

kod_folder_name = 'plugin.video.kod'
url = 'https://github.com/kodiondemand/addon/archive/refs/heads/master.zip'
utils.download_and_extract_plugin(url, kod_folder_name, False)

sys.path.append(os.path.join(utils.full_addons_path(), kod_folder_name))

def run(argv=""):
    to_return = utils.run([argv.split("?")[0], '3', argv.split("?")[1] if "?" in argv else ""])
    movie_class = jclass("CardView")
    movie_list = []
    for item in to_return:
        movie_list.append(movie_class(
            item[0],
            item[1].label,
            item[1].art['thumb'],
            "",
            ""
        ))

    return Arrays.asList(movie_list)
