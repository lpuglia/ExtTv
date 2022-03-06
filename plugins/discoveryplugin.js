var name = "Discovery"
var pluginRequiresProxy = false

var programs = [
    {
        "Title" : "Real Time",
        "Description" : "discovery",
        "Type" : "Live",
        "Logo" : "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/Real_Time_logo.svg/512px-Real_Time_logo.svg.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/playback/v2/channelPlaybackInfo/2?usePreAuth=true",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2021/01/07/1975ce58-c05b-4c0a-9ec3-7ebb05a6c453.jpeg?f=jpeg",
        "TvListings" : "https://disco-api.discoveryplus.it/tvlistings/v2/channels/DiscoveryRealTime?"
    },{
        "Title" : "Nove",
        "Description" : "discovery",
        "Type" : "Live",
        "Logo" : "https://discoverymedia.s3.amazonaws.com/media/canale/nove-1485167045.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/playback/v2/channelPlaybackInfo/3?usePreAuth=true",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2021/01/07/aa9b5920-3f1b-48f1-a840-0adb1e6af276.jpeg",
        "TvListings" : "https://disco-api.discoveryplus.it/tvlistings/v2/channels/Nove?"
    },{
        "Title" : "DMax",
        "Description" : "discovery",
        "Type" : "Live",
        "Logo" : "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/DMAX_-_Logo_2016.svg/512px-DMAX_-_Logo_2016.svg.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/playback/v2/channelPlaybackInfo/5?usePreAuth=true",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2021/01/07/de5b341a-d495-4531-9c30-948b06372c7c.jpeg?f=jpeg",
        "TvListings" : "https://disco-api.discoveryplus.it/tvlistings/v2/channels/DMax?"
    },{
        "Title" : "Giallo",
        "Description" : "discovery",
        "Type" : "Live",
        "Logo" : "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Giallo_-_Logo_2014.svg/512px-Giallo_-_Logo_2014.svg.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/playback/v2/channelPlaybackInfo/6?usePreAuth=true",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2020/11/09/782bdbdb-940f-40e3-b844-235936508faf.jpeg?f=jpeg",
        "TvListings" : "https://disco-api.discoveryplus.it/tvlistings/v2/channels/Giallo?"
    },{
        "Title" : "Fratelli di Crozza",
        "Description" : "discovery",
        "Type" : "OnDemand",
        "Logo" : "https://discoverymedia.s3.amazonaws.com/media/canale/nove-1485167045.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/cms/routes/programmi/fratelli-di-crozza?decorators=viewingHistory&include=default",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2020/11/27/2e4fd5f0-b34b-4416-b631-5b48d2e374cd.png?f=jpeg"
    },{
        "Title" : "Accordi e Disaccordi",
        "Description" : "discovery",
        "Type" : "OnDemand",
        "RequireProxy" : true,
        "Logo" : "https://discoverymedia.s3.amazonaws.com/media/canale/nove-1485167045.png",
        "VideoUrl" : "https://disco-api.discoveryplus.it/cms/routes/programmi/accordi-e-disaccordi?decorators=viewingHistory&include=default",
        "CardImageUrl" : "https://eu2-prod-images.disco-api.com/2020/11/26/4f5746e2-233f-44c0-a318-93e8889936ef.jpeg?f=jpeg"
    }
]

function preBackground(){
    response = android.getResponse("https://disco-api.discoveryplus.it/token?realm=dplayit")
    json = JSON.parse(response)
    cookie_st = json.data.attributes.token
    android.addHeaders(JSON.stringify(dict={"st": cookie_st}));
}


async function getCurrentLiveProgram(url){
    var tvListings
    var image
    for(var i in programs){
        if(programs[i].VideoUrl == url){
            tvListings = programs[i].TvListings
            image = programs[i].CardImageUrl
            break;
        }
    }


    var endDate = new Date();
    var startDate = new Date();
    startDate.setDate(endDate.getDate()-1);
    url = tvListings + "startDate="+startDate.toISOString() + "&endDate="+endDate.toISOString()
    response = await getResponse(url)
    data = JSON.parse(response).data
    currentProgram = data[data.length-1].attributes

    return JSON.stringify(dict = {
                "Title" : currentProgram.showName,
                "Duration" : currentProgram.duration*1000,
                "Description" : currentProgram.description,
                "AirDate" : new Date(currentProgram.utcStart).getTime(),
                "ThumbURL" : image,
                "PageURL" : ""
                })
}

async function getLiveStream(url) {
    response = await getResponse(url)
    json = JSON.parse(response)
    preAuth = json.data.attributes.protection.drmToken
    sourceURL = json.data.attributes.streaming.dash.url
    license_url = "https://discovery-eur.conax.cloud/widevine/license";

    android.playStream(JSON.stringify(dict = {
                      "StreamType": "Dash",
                      "Source": sourceURL,
                      "DRM": "widevine",
                      "License": license_url,
                      "Preauthorization": preAuth
                    }));
}

async function scrapeLastEpisode(url){
    response = await getResponse(url)
    json = JSON.parse(response)

    episodes = json.included
    images = {} // build images map
    episodes.forEach(function(resource) {
        if("type" in resource && resource.type=="image"){
            images[resource.id] = resource.attributes.src
        }
    });

    lastEpisode = episodes[episodes.length - 1]
    id = lastEpisode.relationships.images.data[0].id
    lastEpisode.thumb = images[id]
    return JSON.stringify(lastEpisode)
}

async function scrapeEpisodes(url){
    console.log(url)
    response = await getResponse(url)
    console.log(response)
    results = JSON.parse(response).included

    images = {} // build images map
    results.forEach(function(resource) {
        if("type" in resource && resource.type=="image"){
            images[resource.id] = resource.attributes.src
        }
    });
    //scrape last episode
    lastEpisode = results[results.length - 1]
    id = lastEpisode.relationships.images.data[0].id
    lastEpisode.thumb = images[id]
    addEpisode(JSON.stringify(lastEpisode), true)

    //scrape all the other episodes
    results.forEach(function(result) {
        if(!("component" in result.attributes)) return
        if(!("filters" in result.attributes.component)) return
        seasons = result.attributes.component.filters[0].options
        mandatoryParam = ""
        if("mandatoryParams" in result.attributes.component)
            mandatoryParam = result.attributes.component.mandatoryParams
        programId = result.id
        seasons.forEach(async function(season) {
            param = season.parameter
            counter = 0
            while(true){
                episodesURL = "https://disco-api.discoveryplus.it/cms/collections/" + programId +
                        "?decorators=viewingHistory&include=default&page[items.number]=" + counter + "&" +
                        mandatoryParam + "&" + param;
                response = await getResponse(episodesURL)
                json = JSON.parse(response)
                if("included" in json)
                    episodes = json.included
                else
                    break
                episodes = json.included
                images = {} // build images map
                episodes.forEach(function(resource) {
                    if("type" in resource && resource.type=="image"){
                        images[resource.id] = resource.attributes.src
                    }
                })

                episodes.forEach(function(episode) {
                    if("attributes" in episode && "path" in episode.attributes){

                        arr = episode.attributes.packages[0];
                        if(!(arr == "Free")){
                            return;
                        }

                        id = episode.relationships.images.data[0].id;
                        episode["thumb"] = images[id]
                        addEpisode(JSON.stringify(episode))
                    }
                })
                counter++
            }
        })
    })
}

async function addEpisode(response, play = false, title=""){
    episode = JSON.parse(response)

    var dateParts = episode.attributes.earliestPlayableStart.split("T")[0].split("-")
    d = new Date(+dateParts[0], dateParts[1] - 1, +dateParts[2]).getTime();

    duration = +episode.attributes.videoDuration * 1000
    eId = episode.id; //get last episode id

    var js = JSON.stringify(dict = {
                  "PageURL": "https://disco-api.discoveryplus.it/playback/v2/videoPlaybackInfo/" + eId + "?usePreAuth=true",
                  "ThumbURL": episode.thumb,
                  "AirDate": d,
                  "Description": episode.attributes.description,
                  "Duration": duration,
                  "Title": episode.attributes.name
                });
    android.handleEpisode(js, play, title)
}

function scrapeVideo(url){
    response = android.getResponse(url)
    json = JSON.parse(response)

    sourceURL = json.data.attributes.streaming.hls.url;
    return JSON.stringify(dict = {
            "StreamType": "Hls",
            "Source": sourceURL,
            "DRM": "clearkey",
            "License": "https://dplay-clearkey-service-prod.nep.ms/keys/1.clearkey",
        });
}