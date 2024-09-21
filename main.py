from asyncio.subprocess import Process

import discord
import speech_recognition as sr
import pyttsx3
from discord import VoiceClient
from groq import Groq
from speech_recognition import AudioSource

message_history = []
allowed_users = [546194587920760853]
allowed_servers = [691875797945810976]
command_syntax = "?"

# def contains_word(s, w):
#     return (' ' + w + ' ') in (' ' + s + ' ')
#
# def retrieve_args(content):
#
#
# def allow_server(server_id):
#     if not server_id in allowed_servers:
#         allowed_servers.append(server_id)
#
# def allow_user(user_id):
#     if not user_id in allowed_users:
#         allowed_users.append(user_id)
#
# recognized_commands = {
#     "server": allow_server,
#     "user": allow_user
# }
#
# def is_command(content):
#     if content[:1] is command_syntax:
#         for key in recognized_commands.keys():
#             if contains_word(content, key):
#
#                 recognized_commands.get(content)()
content ='''
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
'''
speaking = False
max_messages = 25 # when to start freeing up context
model="llama-3.1-70b-versatile"
max_tokens=8000
default_history = [
    {
        "role": "system",
        "content": content
        # "content": "You are an engineer human from Gen Z who talks super chill and like a bro. Speak within one paragraph (preferably one to two sentences) and do not yap."
    }
]
history = default_history.copy()
voice_client : VoiceClient = None

def speech_callback(recognizer, audio):
    global speaking
    global history
    if speaking:
        return
    # received audio data, now we'll recognize it using Google Speech Recognition
    try:
        # for testing purposes, we're just using the default API key
        # to use another API key, use `r.recognize_google(audio, key="GOOGLE_SPEECH_RECOGNITION_API_KEY")`
        # instead of `r.recognize_google(audio)`
        text = recognizer.recognize_google(audio)
        print("Google Speech Recognition thinks you said " + text)
        global voice_client
        if voice_client is not None:
            history.append(
                {
                    "role": "user",
                    "content": text
                }
            )
            chat_completion = gClient.chat.completions.create(
                messages=history,
                model="llama3-8b-8192",
                max_tokens=8192
            )
            history.append(
                {
                    "role": "assistant",
                    "content": chat_completion.choices[0].message.content
                }
            )
            # if dClient.voice_client is not None:
            #     dClient.voice_client.play()
            speaking = True
            print('spoke')
            # engine.save_to_file(chat_completion.choices[0].message.content, 'speech.wav')
            # # engine.say(chat_completion.choices[0].message.content)
            # engine.startLoop(False)
            # engine.iterate()
            # engine.endLoop()
            print('saved')
            speaking = False
            print('stopped spekaing')

    except sr.UnknownValueError:
        print("Google Speech Recognition could not understand audio")
    except sr.RequestError as e:
        print("Could not request results from Google Speech Recognition service; {0}".format(e))

class MyDiscordClient(discord.Client):
    # async def start_listening():

    async def on_ready(self):
        print('Logged on as', self.user)
        # guild = await self.fetch_guild(691875797945810976)
        # voice_channel = await self.fetch_channel(691875798466035735)
        # global voice_client
        # voice_client = await voice_channel.connect()
        # voice_client.
        # self.voice_client.play()
        # voice_channel = self.fetch_channel(691875798466035735)
        # voice_client = await voice_channel.connect()

    async def on_message(self, message : discord.Message):
        # only respond to ourselves
        if message.author.id in allowed_users or message.guild.id in allowed_servers:
            fetched_id = 0
            if message.reference is not None:
                fetched_message = await message.channel.fetch_message(message.reference.message_id)
                fetched_id = fetched_message.author.id
            if self.user.id in message.raw_mentions or fetched_id is self.user.id:
                global history
                if 'restart' in message.content:
                    await message.channel.send("Restarting history!")
                    history = default_history.copy()
                else:
                    content = message.content
                    if len(history) >= max_messages:
                        del history[1]
                    history.append(
                        {
                            "role": "user",
                            "content": message.author.display_name+": "+message.content
                        }
                    )
                    chat_completion = gClient.chat.completions.create(
                        messages=history,
                        model=model,
                        max_tokens=max_tokens
                    )
                    history.append(
                        {
                            "role": "assistant",
                            "content": chat_completion.choices[0].message.content
                        }
                    )
                    print(history)
                    try:
                        await message.channel.send(chat_completion.choices[0].message.content)
                    except Exception as e:
                        await message.channel.send("Failed to send a response due to an exception :< sowwy.")
            # if '?' in message.content:
#
# engine = pyttsx3.init()
# engine.setProperty('volume', 1.0)
# voices = engine.getProperty('voices')
# engine.setProperty('voice', voices[1].id)
# engine.runAndWait()
#
# r = sr.Recognizer()
# m = sr.Microphone()
# with m as source:
#     r.adjust_for_ambient_noise(source)  # we only need to calibrate once, before we start listening
#
# # start listening in the background (note that we don't have to do this inside a `with` statement)
# stop_listening = r.listen_in_background(m, speech_callback)

# `stop_listening` is now a function that, when called, stops background listening

gClient = Groq(
    api_key='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'
)
dClient = MyDiscordClient()
dClient.run('OTM5NTkyNjIyOTE2Mzk5MTA0.GaU0wS.CbeF4bWOnXk8wpeCfLmb-CwajQVZv5ZvZUwG14')