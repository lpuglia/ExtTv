var name = "Rai"
var pluginRequiresProxy = false

var programs = [
    {
        "Title" : "Rai 1",
        "Description" : "rai",
        "Type" : "Live",
        "RequireProxy" : true,
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473661951374Logo-Rai1.png",
        "CardImageUrl" : "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Rai_1_-_Logo_2016.svg/512px-Rai_1_-_Logo_2016.svg.png",
        "VideoUrl" : "https://mediapolis.rai.it/relinker/relinkerServlet.htm?cont=2606803&output=56"
    },{
        "Title" : "Rai 2",
        "Description" : "rai",
        "Type" : "Live",
        "RequireProxy" : true,
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473662585214Logo-Rai2.png",
        "CardImageUrl" : "https://upload.wikimedia.org/wikipedia/commons/thumb/9/99/Rai_2_-_Logo_2016.svg/512px-Rai_2_-_Logo_2016.svg.png",
        "VideoUrl" : "https://mediapolis.rai.it/relinker/relinkerServlet.htm?cont=308718&output=56"
    },{
        "Title" : "Rai 3",
        "Description" : "rai",
        "Type" : "Live",
        "RequireProxy" : true,
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473662801274Logo-Rai3.png",
        "VideoUrl" : "https://mediapolis.rai.it/relinker/relinkerServlet.htm?cont=308709&output=56",
        "CardImageUrl" : "https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/Rai_3_-_Logo_2016.svg/512px-Rai_3_-_Logo_2016.svg.png"
    },{
        "Title" : "Presadiretta",
        "Description" : "rai",
        "Type" : "OnDemand",
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473662801274Logo-Rai3.png",
        "VideoUrl" : "https://www.raiplay.it/programmi/presadiretta.json",
        "CardImageUrl" : "https://www.raiplay.it/cropgd/1440x810/dl/img/2020/01/31/1580475813999_Presadiretta_2048x1152.jpg"
    },{
        "Title" : "Report",
        "Description" : "rai",
        "Type" : "OnDemand",
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473662801274Logo-Rai3.png",
        "VideoUrl" : "https://www.raiplay.it/programmi/report.json",
        "CardImageUrl" : "https://www.raiplay.it/cropgd/1024x576/dl/img/2020/10/23/1603468212314_2048x1152.jpg"
    }
]

async function getLiveStream(url) {
    response = await getResponse(url)
    sourceURL = response.split("<![CDATA[")[1].split("]]><")[0]

    if(sourceURL.includes(".mp4"))
        android.playStream(JSON.stringify(dict = {
                              "StreamType": "Extractor",
                              "Source": sourceURL
                            }));
    else
        android.playStream(JSON.stringify(dict = {
                              "StreamType": "Hls",
                              "Source": sourceURL
                            }));
}

async function scrapeLastEpisode(url, title){
    response = await getResponse(url)
    json = JSON.parse(response)
    return "https://www.raiplay.it/" + json.first_item_path
}

async function scrapeEpisodes(url){
    response = await getResponse(url)
    json = JSON.parse(response)
    //scrape last episode
    addEpisode("https://www.raiplay.it/" + json.first_item_path, true);
    blocks = json.blocks
    for (i = 0; i < blocks.length; i++) {
        block = blocks[i]
        publishingBlock = block.id
        contentSet = block.sets[0].id;
        seasonURL = url.replace(".json", "/" + publishingBlock + "/" + contentSet + "/episodes.json");
        response = await getResponse(seasonURL, "scrapeEpisodesAsync");
        reader = JSON.parse(response);
        cards = reader.seasons[i].episodes[0].cards
        for(var j = 0; j < cards.length; j++) {
            addEpisode("https://www.raiplay.it/" + cards[j].path_id)
        }
    }
}

async function addEpisode(url, play = false, title = ""){
    response = await getResponse(url);
    json = JSON.parse(response)

    var dateParts = json.date_published.split("-");
    date_published = new Date(+dateParts[2], dateParts[1] - 1, +dateParts[0]).getTime(); 

    var hms = json.video.duration;   // your input string
    var a = hms.split(':'); // split it at the colons
    // minutes are worth 60 seconds. Hours are worth 60 minutes.
    var duration = ((+a[0]) * 60 * 60 + (+a[1]) * 60 + (+a[2])) * 1000;

    var js = JSON.stringify(dict = {
                  "PageURL": json.video.content_url + "&output=56",
                  "ThumbURL": "https://www.raiplay.it/" + json.images.landscape,
                  "AirDate": date_published,
                  "Description": json.description,
                  "Duration": duration,
                  "Title": json.episode_title
                });
    android.handleEpisode(js, play, title)
}

function scrapeVideo(url){
    response = android.getResponse(url);
    sourceURL = response.split("<![CDATA[")[1].split("]]><")[0]

    return JSON.stringify(dict = {
                          "StreamType": "Hls",
                          "Source": sourceURL
                        });
}