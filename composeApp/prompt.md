When I send a prompt in the chat screen, it seems to get sent but the response is not properly streamed. I have already tried setting a higher timeout, but it seems that the response time is not the issue, because even with the new timeout of 120s the timeout error still appears. I will share the full logcat with you. it seems that the chat screen is not "listening" or actively "waiting" for any responses from the api. When I go to another session and then come back, the response appears. please carefully analyze that.
after we have sent a test prompt and the timeout error happened. I went to another chat and then went back, the answer appeared!
Maybe we have to wait longer? How can we make this more robust? You have to deeply analyze how we can optimize this caht experience properly using all the latest best-practices, features and methods.

I want to get the response streamed like in all modern LLM chat apps, with a nice animation token by token or word by word. Not just BAM heres the FULL ANSWER. I want you to deeply analyze the following repositories.
I have attached the official opensource opencode repository, this is the desktop app for that this MOCCA mobile app is being built. MOCCA should connect to the running opencode server "opencode serve --port 4096" and then ba able to fully control opencode with the mobile device in the MOCCA app. You can deeply analyze the full opencode source code to find out every information you might need, especially the code for the server and api might be interesting. Also oyu can check openchamber/web and its server implementation, it is a web app also built to connect to opencode, but it has its own server/deamon.
Deeply analyze the relevant sourcecode and research how we can implement a performant and robust chat experience with a beautiful response input stream. It shoudl instantly visualize everything that comes from the opencode server.

Carefully analyze all attached links:
https://github.com/anomalyco/opencode
https://github.com/anomalyco/opencode/blob/dev/packages/function/src/api.ts
https://github.com/btriapitsyn/openchamber/tree/main/packages/web