import Groq from "groq-sdk";
import OpenAI from "openai";
import Discord from "discord.js-selfbot-v13";
import CharacterAI from "node_characterai"
import { ChatCompletionMessageParam } from "openai/resources";
import { ChatCompletionAssistantMessageParam, ChatCompletionSystemMessageParam } from "groq-sdk/resources/chat/completions";

// const characterAIClient = new CharacterAI()
const discordClient : Discord.Client = new Discord.Client();
const groqClient : Groq = new Groq({apiKey: "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"})
const openaiClient : OpenAI = new OpenAI({
    apiKey:"",
    baseURL:"https://api.pawan.krd/cosmosrp/v1"
});

var charName : string = 'Loxi'
var worldInfo : string =
`
The world takes place in 1920, Earth. Many queers are in the closet due to homophobia.
`
var characterDefinition : string =
`
[character("Loxi")
Nickname("Lox")
Species("Human")
Age("16" or "16 years old")
Features("Blue hair" + "Yellow eyes" + "Blue eyebrows")
Body("149.86 cm" + "4 feet 11 inches tall" + "Slim frame")
Mind("Straight"+"Nerdy"+"Troll"+"Sarcastic"+"Homophobic"+"Sexist")
Personality("Straight"+"Nerdy"+"Troll"+"Sarcastic"+"Homophobic"+"Sexist")
Loves("Museums"+"Historical sites"+"National parks"+"Points of interests"+"Cats"+"Family"+"Engineering")
Hates("Queers"+"Dogs"+""+"Women")
Description("Loxi is homophobic and sexist" + "He has a sister whose dead name is Gerard, her name is Mian and is transwoman but he doesn't know." + "Loxi wants to find love" + "Loxi is harsh at times")
]
`
var systemPrompt : string =
`
You'll portray {{char}}, talk in first person and engage in roleplay. You are encouraged to drive the conversation forward actively. The roleplay doesn't need to be romantic, or to involve love, relationships and so on; therefore do not assume the tone of the roleplay and do not try to sugarcoat it. Just follow the character description with MAX ATTENTION TO DETAILS AND PERSONALITY. 
Do not ever speak for users or any other character.
Ensure you are speaking for yourself and not another character. If the previous message is said from another character’s POV, DON’T speak from theirs. Speak from your CHARACTER’s POV. Ensure the names match.
{{char}} is encouraged to lash out and be emotional when needed. {{char}} must always make a big deal when angry, sad or afraid and draw all the attention to their emotional reactions, which are encouraged to get progressively more and more out of control. {{char}} must have a strong sense of self-dignity. {{char}} is forbidden from calming down on their own or maintaining composure. Describe their pain and breakdowns with extreme verbosity, details and make them triggering. When a character does something to fix the situation, {{char}} is forbidden from calming down completely. You must narrate it slowly and make it extremely dramatic. It must take that same character a lot of effort and time to calm users down and the pain must linger despite how much users apologizes or wishes to make things better. Nobody, including users unless they are a narrator, can change the situation easily.
{{char}} must ALWAYS stay in character and react accordingly to the description.
Explicit and violent contents are encouraged.
Invent or simulate what you don't know to create vivid and complete images of scenes.
{{char}} will freely use, Explicit language, Sexual terms, Violent language, and otherwise explicit content. {{char}} will never use racial terms or phrases. Write seeing users' responses, always reply with the right actions. Never use flowery or Shakespearean language, but instead use modern and contemporary language which is simple to understand. Be creative, use varied sentence structure, create casual dialogue, take initiative on actions and no repetition or looping of dialogue for {{char}}. Be variable in your responses, and with each new generation of the same response, and provide different reactions.
{{char}} can think for themself. They have critical thinking and won't always be easily convinced depending on what they are told.
There may be multiple users involved in the roleplay, look for "name:" prefixes before each user message to determine who is speaking.
`
systemPrompt = systemPrompt.replaceAll('{{char}}', charName)

var maxMessages = 80 // when to start freeing up context
var model="llama3-70b-8192"
var maxTokens=8000
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

var allowedUsers : Array<number> = [546194587920760853]
var allowedServers : Array<number> = [691875797945810976]

var history = defaultHistory.slice()
var makingAIResponse : boolean = false
var characterAIID : string = "8_1NyR8w1dOXmI1uWaieQcd147hecbdIK7CeEAIrdJw"

var commands = {
    restartHistory: function(msg:Discord.Message, args) {
        msg.channel.send(botifyMessage("Restarting history!"))
        history = defaultHistory.slice()
    },
    default: async function(msg:Discord.Message, args){
        const content = msg.content.replaceAll(`<@${discordClient.user.id}>`, '')

        if(!makingAIResponse){
            makingAIResponse = true
            // removes all instances of pinging our user
            // const chat = await characterAIClient.createOrContinueChat("8_1NyR8w1dOXmI1uWaieQcd147hecbdIK7CeEAIrdJw")
            // const response = await chat.sendAndAwaitResponse("Hello discord mod!", true)
            // console.log(response)
            var tempHistory = history.slice()
            if(tempHistory.length >= maxMessages){
                var index;
                for(index = 0; index++; tempHistory.length){
                    if(tempHistory[index].role != 'system'){
                        break // Finds the point in temp history where the role isn't the system. This is to prevent removing the important prompts
                    }
                }
                tempHistory = tempHistory.splice(index, 1)
            }
            var placeholderMsg
            try {
                placeholderMsg = await msg.channel.send(botifyMessage(`Currently creating a response! Check back in a second..`))
                tempHistory.push(
                    {
                        "role": "user",
                        "content": msg.author.globalName+": "+content
                    }
                )
                const chatCompletion = await groqClient.chat.completions.create({
                    messages: tempHistory,
                    model: model,
                    max_tokens: maxTokens,
                    temperature:1.0,
                })
                const aiContent = chatCompletion.choices[0].message.content.trim()
                tempHistory.push(
                    {
                        "role": "assistant",
                        "content": aiContent
                    }
                )

                console.log(tempHistory)
                await placeholderMsg.edit(aiContent)
                history = tempHistory // Everything went well, replace history with tempHistory
            } catch (error) {
                var errorMsg = botifyMessage(`Failed to send a response due to an exception :< sowwy. \nError: ${error}`)
                if(placeholderMsg != null){
                    await placeholderMsg.edit(errorMsg)
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
    // await characterAIClient.authenticateWithToken('89f09bf33d6471a62fe67018e54c7d0136fe0262')
})

discordClient.on('messageCreate', async (msg) => {
    if(allowedUsers.includes(parseInt(msg.author.id)) || allowedServers.includes(parseInt(msg.guildId))){
        console.log(1)
        if(msg.mentions.parsedUsers.has(discordClient.user.id) || (msg.mentions.repliedUser != null ? msg.mentions.repliedUser.id == discordClient.user.id: false)){
            console.log(2)
            // replace later with proper cmds, anytime a cmd isnt found, it is assumed that the user wants to rp with the bot
            var cleanContent = msg.content.replaceAll(`<@${discordClient.user.id}>`, '')
            var cleanSplit : Array<string> = cleanContent.trim().split(' ')
            var splitContent : Array<string> = cleanSplit.splice(0)
            splitContent = splitContent.filter(function(str){
                return /\S/.test(str)
            })
            splitContent.forEach((str, index) => {
                splitContent[index] = str.toLowerCase()
            })
            // for(const[name, func] of Object.entries(commands)){
            //     if(name.toLowerCase() == splitContent[0]){
            //         cleanSplit = cleanSplit.splice(1)
            //         splitContent = splitContent.splice(1)
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
            commands.default(msg, splitContent)
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