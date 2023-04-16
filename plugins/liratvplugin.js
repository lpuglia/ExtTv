var name = "LiraTv"
var pluginRequiresProxy = false

var programs = [
    {
        "Title" : "LiraTv",
        "Description" : "liratv",
        "Type" : "Live",
        "Logo" : "https://pbs.twimg.com/profile_images/1370923532/logo_lira_400x400.gif",
        "VideoUrl" : "https://a928c0678d284da5b383f29ecc5dfeec.msvdn.net/live/S57315730/8kTBWibNteJA/playlist.m3u8",
        "CardImageUrl" : "https://www.liratv.it/wp-content/uploads/2022/10/liratv_logo.png"
    },{
        "Title" : "LiraTG",
        "Description" : "liratv",
        "Type" : "OnDemand",
        "Logo" : "https://pbs.twimg.com/profile_images/1370923532/logo_lira_400x400.gif",
        "VideoUrl" : "https://www.liratv.it/programmi/liratg-cronaca/",
        "CardImageUrl" : "https://www.liratv.it/wp-content/uploads/2023/04/1681648200_LIRATG-Cronaca-16042023_1681648492-585x329.jpg"
    }
]

async function getLiveStream(url) {
    android.playStream(JSON.stringify(dict = {
                          "StreamType": "Hls",
                          "Source": url
                      }));
}

async function scrapeLastEpisode(url){
    response = await getResponse(url);
    var parser = new DOMParser();
    var htmlDoc = parser.parseFromString(response, 'text/html');
    //scrape last episode
    return htmlDoc.querySelector("article > div > a.icon-post-format").getAttribute("href");
}

async function scrapeEpisodes(url){
    response = await getResponse(url);
    var parser = new DOMParser();
    var htmlDoc = parser.parseFromString(response, 'text/html');
    //scrape last episode
    addEpisode(htmlDoc.querySelector("article > div > a.icon-post-format").getAttribute("href"), true);

    as = htmlDoc.querySelectorAll("article > div > a.icon-post-format")
    as.forEach(element => addEpisode(element.getAttribute("href")));
}

async function addEpisode(url, play = false, title = ""){
    response = await getResponse(url);
    var parser = new DOMParser();
    var doc = parser.parseFromString(response, 'text/html');

    dateVideo = doc.querySelector("time.entry-date").getAttribute("datetime")

    var dateParts = dateVideo.split("T")[0].split('-');
    date = new Date(+dateParts[0], dateParts[1], +dateParts[2]).getTime(); 

    var js = JSON.stringify(dict = {
                  "PageURL": url,
                  "ThumbURL": doc.querySelector("video").getAttribute("poster"),
                  "AirDate": date,
                  "Description": doc.querySelector("#penci-post-entry-inner > p").textContent,
                  "Duration": 0,
                  "Title": doc.querySelector("h1.post-title").textContent,
                });
    android.handleEpisode(js, play, title)
}

function scrapeVideo(url){
    console.log(url)
    response = android.getResponse(url);
    streamURL = response.split("strCurrentVideoUrl = '")[1].split("';")[0]
    js = JSON.stringify(dict = {
                  "StreamType": "Default",
                  "Source": streamURL
                });
    return js;
}
