var name = "Mediaset"
//var pluginRequiresProxy = false
var mediasetExta = "?format=smil&auto=true&tracking=true&balance=true&formats=MPEG-DASH%2CM3U&assetTypes=HD%2Cbrowser%3ASD%2Cbrowser&auth=eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJtZWRpYXNldC1wcm9kL21lZGlhc2V0LXByb2QvYW5vbnltb3VzIiwiaXNzIjoiMSIsImV4cCI6MTYxMzI5NjQ5MywiaWF0IjoxNjEzMjEwMDkzMjMyLCJqdGkiOiI3NWZlMjk0NC0zMzI3LTRiMjktYTBlNC1iMmZmZDUxMmExZjQiLCJkaWQiOiJtZWRpYXNldC1wcm9kIiwidW5tIjoiYW5vbnltb3VzIiwiY3R4Ijoie1widXNlck5hbWVcIiA6IFwibWVkaWFzZXQtcHJvZC9hbm9ueW1vdXNcIiwgXCJkZXZpY2VcIjogXCJwY1wiLCBcInBsYXRmb3JtXCI6XCJhdm9kXCIsXCJ0eXBlXCI6XCJ1bnJlZ2lzdGVyZWRcIixcImNvdW50cnlcIjogXCJJVFwifVxuIiwib2lkIjpudWxsfQ.WjyKNMI3ZNotFShGnlHsDvhsoj_o1exLH3AYKJ8I0up_ygj4jImqlavzQQL9NgOoI3gG5nAonKnBVozO_9nxnCGFtV6ifm6FrGUDCvWasPuULA75c-yeDO_MMPEg9_43op2RHL9FRU7RSlJ5Jicq7lY5pedfdNFI1Sn8wf55mhKBAW044JISkOyb5QBRRntowBpNeeuvjFVX_l7Kim835jhoQGA78PNh5Ay5GjgNHknyWFZv4BCIDS2hR86IMe-xJ2wddVNNV6QDQpeg_2yp5PYhscHceQ6U-mpmPWCqPMFeyS6ZS80v5Q7J0x6VLkB-bUfHnAmjUdu4osljwlAlLg";

var programs = [{
        "Title" : "Rete 4",
        "Description" : "mediaset",
        "Type" : "Live",
        "RequireProxy" : true,
        "VideoUrl" : "https://link.theplatform.eu/s/PR1GhC/h7J0pAWfnE5w" + mediasetExta,
        "Logo" : "https://cdn.one.accedo.tv/files/5b9940daa6f547000c07c39e",
        "CardImageUrl" : "https://tv.upgo.news/wp-content/uploads/2020/07/nuovologo-rete4.jpg"
    },{
        "Title" : "Canale 5",
        "Description" : "mediaset",
        "Type" : "Live",
        "RequireProxy" : true,
        "VideoUrl" : "https://link.theplatform.eu/s/PR1GhC/aVIT5Z5LuPa1" + mediasetExta,
        "Logo" : "https://cdn.one.accedo.tv/files/5b1e2ea5a0e845000cd36e55",
        "CardImageUrl" : "https://www.monkeytalkie.com/wp-content/uploads/2018/04/IMG_02.jpg"
    },{
        "Title" : "Italia 1",
        "Description" : "mediaset",
        "Type" : "Live",
        "RequireProxy" : true,
        "VideoUrl" : "https://link.theplatform.eu/s/PR1GhC/ZDbCaAqpagXc" + mediasetExta,
        "Logo" : "https://cdn.one.accedo.tv/files/5ae6ec03a0e845000cd36a5b",
        "CardImageUrl" : "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR4X8subiNP39Ko22lOm36IIfdSifXFbjGe0Q&usqp=CAU"
    },{
        "Title" : "Top Crime",
        "Description" : "mediaset",
        "Type" : "Live",
        "RequireProxy" : true,
        "VideoUrl" : "https://link.theplatform.eu/s/PR1GhC/4F3wY7ZM6yur" + mediasetExta,
        "CardImageUrl" : "https://cdn.one.accedo.tv/files/5b2d2f8c23eec6000dd56ff1"
    }
]

async function getCurrentLiveProgram(url){
    var title
    var image
    for(var i in programs){
        if(programs[i].VideoUrl == url){
            title = programs[i].Title
            image = programs[i].CardImageUrl
            break;
        }
    }

    timestamp = Date.now()
    liveInfo = `https://feed.entertainment.tv.theplatform.eu/f/PR1GhC/mediaset-prod-all-listings?byListingTime=${timestamp}~${timestamp+1000}&byCallSign=`
    response = await getResponse(liveInfo);
    entries = JSON.parse(response).entries
    for(var i = 0; i < entries.length; i++){
        entry = entries[[i]]
        if(entry.title == title){
            for(var j = 0; j < entry.listings.length; j++){
                listing = entry.listings[j]
                if(timestamp>listing.endTime) continue

                var thumb = listing.program.thumbnails["image_keyframe_poster-1280x720"]
                return JSON.stringify(dict = {
                    "Title" : listing.mediasetlisting$epgTitle,
                    "Duration" : listing.endTime - listing.startTime,
                    "Description" : listing.description,
                    "AirDate" : listing.startTime,
                    "ThumbURL" : typeof thumb !== 'undefined' ? thumb.url : image,
                    "PageURL" : ""
                    }
                )
            }
        }
    }
}

async function getLiveStream(url) {
    response = await getResponse(url);
    sourceURL = response.split("<video src=\"")[1].split("\" ")[0]
    android.playStream(JSON.stringify(dict = {
                          "StreamType": "Dash",
                          "Source": sourceURL
                        }));
}
