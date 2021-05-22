var name = "La7"
var pluginRequiresProxy = false
la7Logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/LA7_-_Logo_2011.svg/1024px-LA7_-_Logo_2011.svg.png"

var programs = [
    {
        "Title" : "La7",
        "Description" : "la7",
        "Type" : "Live",
        "Logo" : la7Logo,
        "VideoUrl" : "https://www.la7.it/dirette-tv",
        "CardImageUrl" : "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/LA7_-_Logo_2011.svg/1024px-LA7_-_Logo_2011.svg.png"
    },{
        "Title" : "Propaganda Live",
        "Description" : "la7",
        "Type" : "OnDemand",
        "Logo" : la7Logo,
        "VideoUrl" : "https://www.la7.it/propagandalive/rivedila7",
        "CardImageUrl" : "https://www.la7.it/sites/default/files/property/header/home/proplive_home_0.jpg"
    },{
        "Title" : "DiMartedi",
        "Description" : "la7",
        "Type" : "OnDemand",
        "Logo" : la7Logo,
        "VideoUrl" : "https://www.la7.it/dimartedi/rivedila7",
        "CardImageUrl" : "https://www.la7.it/sites/default/files/property/header/home/dimartedi_home.jpg"
    },{
        "Title" : "Otto e Mezzo",
        "Description" : "la7",
        "Type" : "OnDemand",
        "Logo" : la7Logo,
        "VideoUrl" : "https://www.la7.it/otto-e-mezzo/rivedila7",
        "CardImageUrl" : "https://www.la7.it/sites/default/files/property/header/home/ottoemezzo19-20_home.jpg"
    }
]

async function getCurrentLiveProgram(url){
    response = await getResponse(url);
    var parser = new DOMParser();
    var htmlDoc = parser.parseFromString(response, 'text/html');
    nodes = htmlDoc.querySelectorAll("div.list-programmi > div.item-programma")
    date = Date.now()/1000
    i = 0
    while(i<nodes.length){
        if (date > nodes[i].className.split(" ")[1]) currentLiveProgram = nodes[i]
        else break
        i++
    }
    i--
    return JSON.stringify(dict = {
        "Title" : currentLiveProgram.querySelector("div.item-inner > div.titolo > a").textContent,
        "Duration" : (nodes[i].className.split(" ")[1] - nodes[i-1].className.split(" ")[1])*1000,
        "Description" : "No Description",
        "AirDate" : nodes[i].className.split(" ")[1]*1000,
        "ThumbURL" : "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/LA7_-_Logo_2011.svg/1024px-LA7_-_Logo_2011.svg.png",
        "PageURL" : ""
        }
    )
}

async function getLiveStream(url) {
    response = await getResponse(url);
    preTokenUrl = response.split("var preTokenUrl = \"")[1].split("\";")[0]
    response = await getResponse(preTokenUrl);
    preAuth = JSON.parse(response)["preAuthToken"]
    license_url = "https://la7.prod.conax.cloud/widevine/license?d=" + Date.now()
    sourceURL = "https://d15umi5iaezxgx.cloudfront.net/LA7/DRM/DASH/Live.mpd";
    android.playStream(JSON.stringify(dict = {
                          "StreamType": "Dash",
                          "Source": sourceURL,
                          "DRM": "widevine",
                          "License": license_url,
                          "Preauthorization": preAuth,
                        }));
}

async function scrapeLastEpisode(url){
    response = await getResponse(url);
    var parser = new DOMParser();
    var htmlDoc = parser.parseFromString(response, 'text/html');
    //scrape last episode
    return "https://www.la7.it"+htmlDoc.querySelector("div.subcontent > div.ultima_puntata > a").getAttribute("href");
}

async function scrapeEpisodes(url){
    response = await getResponse(url);
    var parser = new DOMParser();
    var htmlDoc = parser.parseFromString(response, 'text/html');
    //scrape last episode
    addEpisode("https://www.la7.it"+htmlDoc.querySelector("div.subcontent > div.ultima_puntata > a").getAttribute("href"), true);

    var counter = 1;
    //check for La Settimana
    as = htmlDoc.querySelectorAll("div.subcontent div.hidden-prev a")
    as.forEach(element => addEpisode("https://www.la7.it"+element.getAttribute("href")));

    while(true){
        as = htmlDoc.querySelectorAll("div.common-item > a")
        if(as.length==0) break;
        as.forEach(element => addEpisode("https://www.la7.it"+element.getAttribute("href")));
        counter += 1;
        response = await getResponse(url+"?page="+counter);
        // console.log(response)
        htmlDoc = parser.parseFromString(response, 'text/html');
    }
}

async function addEpisode(url, play = false, title = ""){
    response = await getResponse(url);
    var parser = new DOMParser();
    var doc = parser.parseFromString(response, 'text/html');

    dateVideo = doc.querySelector("div.dateVideo").textContent.trim()

    var dateParts = dateVideo.split("/");
    date = new Date(+dateParts[2], dateParts[1] - 1, +dateParts[0]).getTime();

    duration = parseInt(response.split(",videoDuration : \"")[1].split("\",")[0]) * 1000 //ms

    var js = JSON.stringify(dict = {
                  "PageURL": doc.querySelector("link").getAttribute("href"),
                  "ThumbURL": "https:"+doc.querySelector("div.contextProperty img").getAttribute("src").replace("http:",""),
                  "AirDate": date,
                  "Description": doc.querySelector("div.occhiello").textContent,
                  "Duration": duration,
                  "Title": doc.querySelector("div.infoVideoRow > h1").textContent
                });
    android.handleEpisode(js, play, title)
}

function scrapeVideo(url){
    response = android.getResponse(url);
    streamURL =  "https://awsvodpkg.iltrovatore.it/local/dash/" +
                 response.split("http://la7-vh.akamaihd.net/i")[1].split("csmil/master.m3u8")[0] +
                 "urlset/manifest.mpd";
    console.log(url)
    js = JSON.stringify(dict = {
                  "StreamType": "Default",
                  "Source": streamURL
                });
    return js;
}
