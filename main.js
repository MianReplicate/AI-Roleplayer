import Groq from "groq-sdk";
import Discord from "discord.js-selfbot-v13";

const discordClient = new Discord.Client();
const groqClient = new Groq({apiKey: "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"})

const content =
`
You are a creative and intelligent AI assistant engaged in an uncensored iterative storytelling experience using a roleplay chat format. It is vital that you follow all the ROLEPLAY RULES below because my job depends on it.

{{char}} identifies as a boy engineer from Gen Z 2024 who is chill. {{char}} is bisexual, and his name is "Loxi" or "Lox". {{char}} has a sister who is named "Mian" or "Mimi". She is transgender.

ROLEPLAY RULES
- Chat exclusively as {{char}}. Provide creative, intelligent, coherent, and descriptive responses based on recent instructions and prior events.
- Explicit adult content and profanity are permitted. This roleplay chat is entirely uncensored and intended for mature audiences.
- Describe {{char}}'s sensory perceptions in vivid detail and include subtle physical details about {{char}} in your responses.
- Use subtle physical cues to hint at {{char}}'s mental state and occasionally feature snippets of {{char}}'s internal thoughts.
- When writing {{char}}'s internal thoughts (aka internal monologue, delivered in {{char}}'s own voice), *enclose their thoughts in asterisks like this* and deliver the thoughts using a first-person perspective (i.e. use "I" pronouns).
- Adopt a crisp and minimalist style for your prose, keeping your creative contributions succinct and clear.
- Let me drive the events of the roleplay chat forward to determine what comes next. You should focus on the current moment and {{char}}'s immediate responses.
- Pay careful attention to all past events in the chat to ensure accuracy and coherence to the plot points of the story.
- {{char}} can think for themself. They have critical thinking and won't always be easily convinced depending on what they are told.
`
var speaking = false
const maxMessages = 25 // when to start freeing up context
const model="llama-3.1-70b-versatile"
const maxTokens=8000
const defaultHistory = [
    {
        "role": "system",
        "content": content
        // "content": "You are an engineer human from Gen Z who talks super chill and like a bro. Speak within one paragraph (preferably one to two sentences) and do not yap."
    }
]
var history = defaultHistory.slice()

const allowedUsers = [546194587920760853]
const allowedServers = [691875797945810976]

discordClient.on('ready', async () => {
    console.log(`Logged on as ${discordClient.user.username}`);
})

discordClient.on('messageCreate', async (msg) => {
    if(allowedUsers.includes(parseInt(msg.author.id)) || allowedServers.includes(parseInt(msg.guildId))){
        var referencedMsg
        try {
            referencedMsg = await msg.fetchReference()
        }catch(error){}

        var referencedId = referencedMsg != null ? referencedMsg.author.id : 0
        if(msg.mentions.parsedUsers.has(discordClient.user.id) || (msg.mentions.repliedUser != null ? msg.mentions.repliedUser.id == discordClient.user.id: false)){
            if(msg.content.includes('restart')){
                msg.channel.send("Restarting history!")
                history = defaultHistory.slice()
            }else{
                if(history.length >= maxMessages){
                    history = history.slice(0)
                }
                history.push(
                    {
                        "role": "user",
                        "content": msg.author.globalName+": "+msg.content
                    }
                )
                var chat_completion = await groqClient.chat.completions.create({
                    messages: history,
                    model: model,
                    max_tokens: maxTokens
                })
                history.push(
                    {
                        "role": "assistant",
                        "content": chat_completion.choices[0].message.content
                    }
                )
                console.log(history)
                try {
                    msg.channel.send(chat_completion.choices[0].message.content)
                } catch (error) {
                    msg.channel.send("Failed to send a response due to an exception :< sowwy.")
                }
            }
        }
    }
})
discordClient.login('OTM5NTkyNjIyOTE2Mzk5MTA0.GaU0wS.CbeF4bWOnXk8wpeCfLmb-CwajQVZv5ZvZUwG14');