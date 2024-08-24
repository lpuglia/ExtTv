import os
import utils
import traceback

from java import jclass
from java.util import Arrays
main_activity = jclass("com.android.exttv.MainActivity").getInstance()

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

def run(argv=""):
    print([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else "?"])
    movie_list = []
    try:
        to_return = utils.run([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else ""])
        cardView = jclass("com.android.exttv.manager.SectionManager$CardItem")
        movie_list = []
        for item in to_return:
            movie_list.append(cardView(
                item[0],
                item[1].label,
                item[1].label2,
                item[1].info.get('video', {}).get('plot', ''),
                item[1].art.get('thumb', ''),
                item[1].art.get('poster', ''),
                item[1].art.get('fanart', ''),
                item[2] # isFolder
            ))
    except Exception as e:
        traceback.print_exception(type(e), e, e.__traceback__)
        main_activity.showToast(str(e), 1)

    return Arrays.asList(movie_list)
