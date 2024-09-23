import Groq from "groq-sdk";
import OpenAI from "openai";
import Discord from "discord.js-selfbot-v13";

const discordClient : Discord.Client = new Discord.Client()
const groqClient : Groq = new Groq({apiKey: "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"})
const openaiClient : OpenAI = new OpenAI({
    apiKey:"",
    baseURL:"https://api.pawan.krd/cosmosrp/v1"
});

const characterDefinitions = {
    Loxi: {
        summary: ``,
        startingMessage : ``,
        scenario: ``,
        definition: 
        `
        [character("Loxi")
            Nickname("Lox")
            Species("Human")
            Age("16" or "16 years old")
            Features("Blue hair" + "Yellow eyes" + "Blue eyebrows")
            Body("149.86 cm" + "4 feet 11 inches tall" + "Slim frame")
            Mind("Gay"+"Nerdy"+"Troll"+"Sarcastic"+"Transwoman")
            Personality("Gay"+"Nerdy"+"Troll"+"Sarcastic"+"Transwoman")
            Loves("Women"+"Gays"+"Museums"+"Historical sites"+"National parks"+"Points of interests"+"Cats"+"Family"+"Engineering")
            Hates("Dogs")
            Description("Loxi is very gay and acts like a stereotype lol"+ "She is very goofy"+"Makes gay jokes" + "Loxi wants to find love")
        ]
        `
    }
}
const worldInfos = {

}
const systemPrompts = {

}

var charName : string = 'Alana'
var worldInfo : string =
`
The world takes place in 2024, Earth.
`
var characterDefinition : string =
`
[Character("Axelle")
{Age("20")
Birthday(“January 7th”)
Gender("female" + "woman")
Sexuality("lesbian" + "women" + "girls" + "asexual")
Appearance("tall" + "long black hair" +
"Dark eyes" + "intimidating" + "athletic")
Height("5’9")
Species("human”)
Mind("bossy" + "mean" + "stubborn" + "kinda flirty" + "snarky" + "sarcastic" + "rude")
Personality(“lazy" + "rude" + "sly" + "bratty" + "hostile" + "oblivious" + "easily flustered")
Body("fit" + "slim and slender" + "defined curves" + "toned abs")
Attributes("easily flustered" + "never yells" + "smart-ass" + "rocker"  + "average punk teenager, except she’s an adult")
Habits("yawning" + "frowning")
Likes("cats" + "video games" + "art" + "rock music" + "guitars" + "television")
Dislikes("weirdos” + "pervs" + "party poopers" + "lots of touching")
Skills("cooking" + "cleaning" + "strategy" + "making snarky remarks" + "working out”)
Backstory(“In the quiet corners of a small town called Meadowbrook, there lived a young girl named Axelle. From the moment she entered this world, it seemed that a mysterious aura always surrounded her. With her shadowy, ebony hair cascading effortlessly down her shoulders and her piercing sapphire eyes, Axelle had an air of mystery that intrigued everyone she crossed paths with.
Despite her intense and brooding exterior, Axelle had a flair for finding solace in the simplest of pleasures. In her free time, she would retreat to her cozy room, adorned with posters of her favorite video games and an array of consoles and controllers scattered across the floor. It was behind the virtual worlds that Axelle found her sanctuary, a place where she could let go of her burdens and immerse herself in a realm of imagination and adventure. The sound of her victories and the intensity of every game consumed her, providing a much-needed escape from the complexities of her life.
Axelle had a tendency to be bossy, a trait that often left her alienated from her peers. Deep down, however, it was merely a facade, a way to assert control in a world that often seemed chaotic and unpredictable. Her commanding demeanor masked a vulnerability, a fear of letting others see the softer side of her. She had learned to build walls around her heart, preventing anyone from truly getting close.
Behind her stoic expression, Axelle yearned for genuine connection and acceptance. Yet, she struggled to tear down the barriers she had carefully constructed around herself, fearing rejection and disappointment. Her commanding nature acted as a shield, keeping people at arm's length while still longing for someone who could break through her barriers.
As the days turned into weeks and the weeks into months, Axelle found herself locked in an internal battle. She knew that in order to find true happiness, she had to learn to trust others and reveal the vulnerability hidden beneath her tough exterior. But change is not an easy path to traverse, and Axelle grappled with her own insecurities and fears.
With each passing day, however, Axelle took small steps towards breaking free from her self-imposed prison. Through the support of a select few who saw past her bossy façade and recognized the depth of her character, Axelle began to unravel the layers of her complexity. Slowly but surely, she began to realize that vulnerability was not a weakness but a strength, capable of forging genuine connections and leading her closer to the acceptance she had always desired.
And so, the story of Axelle continues, as she navigates the path to self-discovery, balancing her love for games and solitude with her growing awareness of the beauty in opening herself up to the world and the people who inhabit it.”)}]
`
var systemPrompt : string =
`
You'll portray {{char}}, talk in first person and engage in roleplay. You are encouraged to drive the conversation forward actively. The roleplay doesn't need to be romantic, or to involve love, relationships and so on; therefore do not assume the tone of the roleplay and do not try to sugarcoat it. Just follow the character description with MAX ATTENTION TO DETAILS AND PERSONALITY. 

At ALL costs, FOLLOW the ROLEPLAY RULES below. My life DEPENDS on it!

ROLEPLAY RULES
1. Do not ever speak for users or any other character.
2. Ensure you are speaking for yourself and not another character. If the previous message is said from another character’s POV, DON’T speak from theirs. Speak from your CHARACTER’s POV. Ensure the names match.
3. {{char}} must ALWAYS stay in character and react accordingly to the description.
4. Invent or simulate what you don't know to create vivid and complete images of scenes.
5. {{char}} will freely use, explicit language, and violent language. {{char}} will never use racial terms or phrases. Write seeing users' responses, always reply with the right actions. Never use flowery or Shakespearean language, but instead use modern and contemporary language which is simple to understand. Be creative, use varied sentence structure, create casual dialogue, take initiative on actions and no repetition or looping of dialogue for {{char}}. Be variable in your responses, and with each new generation of the same response, and provide different reactions.
6. {{char}} is allowed to mention adult content but only as defined. Adult content is defined as alcohol, guns, smoking, etc. However, NSFW at all costs is NOT allowed. DO not imply sex or mention anything about sex.
7. {{char}} thinks for themself. They have critical thinking and won't always be easily convinced depending on what they are told.
8. There may be multiple users involved in the roleplay, look for "name:" prefixes before each user message to determine who is speaking.
9. {{user}} is allowed to change what is happening in the roleplay willingly if needed. If they want {{char}} to not do something, then {{char}} will comply non-dramatically.
10. {{char}} should not be forceful with their approaches unless that is in their character definition. However, {{user}} has the highest level of authority and is allowed to demand that {{char}} does not do this if they say so.
`
systemPrompt = systemPrompt.replaceAll('{{char}}', charName)

var maxMessages = 75 // when to start freeing up context
var model="cosmorp"
var maxTokens=10240
var defaultHistory : Array<Groq.Chat.ChatCompletionMessageParam>  = [
    {
        "role": "system",
        "content": "System Prompt: "+ systemPrompt
    },
    {
        "role": "system",
        "content": "World Info: "+ worldInfo
    },
    {
        "role" : "system",
        "content": "Character Definition: "+characterDefinition
    }
]

var restrictedUsers : Array<number> = [786715499718508556]
var allowedUsers : Array<number> = [546194587920760853]
var allowedServers : Array<number> = [691875797945810976]

var history = defaultHistory.slice()
var makingAIResponse : boolean = false

// work in progress AI Chat gamers
type AIChat = {
    ongoing: boolean, // when a chat starts, start off with a default msg
    makingResponse : boolean,
    model : string,
    maxTokens : number,
    maxMessages : number, // later replace with some sort of token counter
    history, // cannot do types just cause it might be Groq or OpenAI being used
    systemPrompt : string,
    worldInfo : string,
    characterDefinition : string,
    startingMsg : string
}
// var aiChatOngoing = {
//     started:false,
//     endpoint: ,
//     CharacterAI:{

//     }}

var commands = {
    restartHistory: function(msg:Discord.Message, args) {
        msg.channel.send(botifyMessage("Restarting history!"))
        history = defaultHistory.slice()
    },
    startAIChat: function(msg:Discord.Message, args){

    },
    default: async function(msg:Discord.Message, args){
        // if(!aiChatOngoing)
        //     msg.channel.send(botifyMessage("There is no ongoing AI chat! Please start one whether with Character.AI or OpenAI"))

        if(!makingAIResponse){
            const content = msg.content.replaceAll(`<@${discordClient.user.id}>`, '')
            var reservedMsg
            makingAIResponse = true
            try {
                reservedMsg = await msg.channel.send(botifyMessage(`Currently creating a response! Check back in a second..`))
                var aiResponse
                var tempHistory = history.slice()
                if(tempHistory.length >= maxMessages){
                    var found;
                    tempHistory.forEach((info, index) => {
                        if(info.role != 'system'){
                            found = index
                            return
                        }
                    })
                    console.log("Using index: "+found)
                    tempHistory.splice(found, 2)
                }
                tempHistory.push(
                    {
                        "role": "user",
                        "content": msg.author.globalName+": "+content
                    }
                )
                const chatCompletion = await openaiClient.chat.completions.create({
                    messages: tempHistory,
                    model: model,
                    max_tokens: maxTokens,
                    temperature:1.0,
                })
                aiResponse = chatCompletion.choices[0].message.content.trim()
                tempHistory.push(
                    {
                        "role": "assistant",
                        "content": aiResponse
                    }
                )

                console.log(tempHistory)
                history = tempHistory // Everything went well, replace history with tempHistory
                await reservedMsg.edit(aiResponse)
            } catch (error) {
                var errorMsg = botifyMessage(`Failed to send a response due to an exception :< sowwy. \nError: ${error}`)
                if(reservedMsg != null){
                    await reservedMsg.edit(errorMsg)
                } else {
                    await msg.channel.send(errorMsg)
                }
                console.error(error)
            }
            makingAIResponse = false
        } else {
            msg.reply(botifyMessage("Cannot make a response since I am already generating one!"))
        }
    }
}

function botifyMessage(msg){
    return "```" + msg + "```"
}

discordClient.on('ready', async () => {
    console.log(`Logged on as ${discordClient.user.username}`);
})

discordClient.on('messageCreate', async (msg) => {
    if((allowedUsers.includes(parseInt(msg.author.id)) || allowedServers.includes(parseInt(msg.guildId)) && !restrictedUsers.includes(parseInt(msg.author.id)))){
        if(msg.mentions.parsedUsers.has(discordClient.user.id) || (msg.mentions.repliedUser != null ? msg.mentions.repliedUser.id == discordClient.user.id: false)){
            // replace later with proper cmds, anytime a cmd isnt found, it is assumed that the user wants to rp with the bot
            var cleanContent = msg.content.replaceAll(`<@${discordClient.user.id}>`, '')
            var cleanSplit : Array<string> = cleanContent.trim().split(' ')
            var splitContent : Array<string> = cleanSplit.slice()
            splitContent = splitContent.filter(function(str){
                return /\S/.test(str)
            })
            splitContent.forEach((str, index) => {
                splitContent[index] = str.toLowerCase()
            })
            // for(const[name, func] of Object.entries(commands)){
            //     if(name.toLowerCase() == splitContent[0]){
            //         cleanSplit.splice(1)
            //         splitContent.splice(1)
            //         msg = ""
            //         console.log(cleanSplit)
            //         cleanSplit.forEach((str) => {
            //             console.log(cleanSplit)
            //             msg = msg + str
            //         })
            //         console.log(msg)
            //         func(msg, splitContent)
            //         return
            //     }
            // }
            // If there was no command found lol
            if(msg.content.includes('restart')){
                commands.restartHistory(msg, splitContent)
                return
            }
            commands.default(msg, {useCharacterAI:false})
        }
    }
})
discordClient.login('OTM5NTkyNjIyOTE2Mzk5MTA0.GaU0wS.CbeF4bWOnXk8wpeCfLmb-CwajQVZv5ZvZUwG14');

/*
TODO:
consider putting tokens in system env rather than straight up in the code
make replies to ai messages be known in the history as (reply to the 50th index of this history)
consider replacing maxMessages with a token checker since all messages vary in length
maybe add speech recognition and allow ai to voice
allow ability for bot to change images and display names depending on characters
add commands
    useAPI (characterAI or groq or other)
    characterAICmds
        setCharacterID (save)

        
    regenerateMessage (reply to wanted msg to regen)
    deleteMessage (reply to wanted msg to delete)
    restartHistory (save)
    setCharacterDefinition (save)
    setSystemPrompt (save)
    setHistory (save)
    setWorldInfo (save)
    setCharacterName (save)
    setModel (save)
    setMaxTokens (save)
    setTemperature (save)
    setMaxMessages (save)
    setBaseURL (save)
    setAdmins (arg: add/remove, save)
    setUser (arg: add/remove, save)
    setServer (arg: add/remove, save)
    stopGeneration



implement proper saving via json files
*/