import os
import utils
import traceback

from java import jclass
from java.util import Arrays

toast_utils = jclass("com.android.exttv.util.ToastUtils")

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

def run(argv=""):
    print([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else "?"])
    movie_list = []
    try:
        to_return = utils.run([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else ""])
        cardView = jclass("com.android.exttv.model.data.CardItem")
        movie_list = []
        for item in to_return:
            video = item[1].info.get('video', {})
            plot = video.get('plot', '') if video else ''
            movie_list.append(cardView(
                item[0],
                item[1].label if item[1].label else '',
                item[1].label2 if item[1].label2 else '',
                plot,
                item[1].art.get('thumb', ''),
                item[1].art.get('poster', ''),
                item[1].art.get('fanart', ''),
                item[2], # isFolder
                utils.parent_uri_map[item[0]],
                utils.parent_uri_map[item[0]],
                "", "", False
            ))
    except Exception as e:
        traceback.print_exception(type(e), e, e.__traceback__)
        toast_utils.showToast(str(e), 1)
    return Arrays.asList(movie_list)
