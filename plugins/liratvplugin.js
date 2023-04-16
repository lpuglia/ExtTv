var name = "LiraTv"
var pluginRequiresProxy = false

var programs = [
    {
        "Title" : "LiraTv",
        "Description" : "liratv",
        "Type" : "Live",
        "Logo" : "https://pbs.twimg.com/profile_images/1370923532/logo_lira_400x400.gif",
        "VideoUrl" : "https://a928c0678d284da5b383f29ecc5dfeec.msvdn.net/live/S57315730/8kTBWibNteJA/playlist.m3u8",
        "CardImageUrl" : "https://www.liratv.it/images/logo-h.png"
    }]

async function getLiveStream(url) {
    android.playStream(JSON.stringify(dict = {
                          "StreamType": "Hls",
                          "Source": url
                      }));
}
