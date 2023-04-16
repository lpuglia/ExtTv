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
    },{
        "Title" : "Una Pezza di Lundini",
        "Description" : "rai",
        "Type" : "OnDemand",
        "Logo" : "https://www.raiplay.it/dl/img/2016/09/1473662585214Logo-Rai2.png",
        "VideoUrl" : "https://www.raiplay.it/programmi/unapezzadilundini.json",
        "CardImageUrl" : "https://www.raiplay.it/cropgd/2048x1152/dl/img/2021/04/17/1618684844497_2048x1152.jpg"
    }


]

async function getCurrentLiveProgram(url){
    var title
    for(var i in programs){
        if(programs[i].VideoUrl == url){
            title = programs[i].Title
            break;
        }
    }
    response = await getResponse("https://www.raiplay.it/palinsesto/onAir.json")

    json = JSON.parse(response).on_air
    for(var i in json){
        if(json[i].channel == title){
            var a = json[i].currentItem.duration.split(':');
            var duration = ((+a[0]) * 60 * 60 + (+a[1]) * 60 + (+a[2])) * 1000;

            var offsetUTC = -1
            var DaylightSavingTime = -1 // TO FIX WITH AUTOMATIC DST detection
            a = json[i].currentItem.hour.split(':')
            airHour = ((+a[0] + offsetUTC + DaylightSavingTime)*60*60 + a[1]*60) * 1000
            return JSON.stringify(dict = {
                "Title" : json[i].currentItem.program.name,
                "Duration" : duration,
                "Description" : json[i].currentItem.description,
                "AirDate" : airHour,
                "ThumbURL" : "https://www.raiplay.it/" + json[i].currentItem.image,
                "PageURL" : ""
                }
            )
        }
    }
}

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

async function scrapeLastEpisode(url){
    response = await getResponse(url)
    json = JSON.parse(response)
    // return "https://www.raiplay.it/" + json.first_item_path
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
            return "https://www.raiplay.it/" + cards[j].path_id
        }
    }

}

async function scrapeEpisodes(url){
    response = await getResponse(url)
    json = JSON.parse(response)
    //scrape last episode
    // addEpisode("https://www.raiplay.it/" + json.first_item_path, true);
    blocks = json.blocks
    play = true
    for (i = 0; i < blocks.length; i++) {
        block = blocks[i]
        publishingBlock = block.id
        contentSet = block.sets[0].id;
        seasonURL = url.replace(".json", "/" + publishingBlock + "/" + contentSet + "/episodes.json");
        response = await getResponse(seasonURL, "scrapeEpisodesAsync");
        reader = JSON.parse(response);
        cards = reader.seasons[i].episodes[0].cards
        for(var j = 0; j < cards.length; j++) {
            addEpisode("https://www.raiplay.it/" + cards[j].path_id, play)
            play = false
        }
    }
}

async function addEpisode(url, play = false, title = ""){
    response = await getResponse(url);
    json = JSON.parse(response)

    var dateParts = json.date_published.split("-");
    date_published = new Date(+dateParts[2], dateParts[1] - 1, +dateParts[0]).getTime();

    var a = json.video.duration.split(':');
    var duration = ((+a[0]) * 60 * 60 + (+a[1]) * 60 + (+a[2])) * 1000;

    if(isNaN(duration)) duration = 60*10*1000

    if(duration>=60*10*1000){
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
}

function scrapeVideo(url){
    response = android.getResponse(url);
    sourceURL = response.split("<![CDATA[")[1].split("]]><")[0]

    return JSON.stringify(dict = {
                          "StreamType": "Hls",
                          "Source": sourceURL
                        });
}
