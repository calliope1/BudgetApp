def init_secrets(path : str):
    key = input("Enter shared secret key (will be visible on screen). Alternatively, leave blank to generate a random one (or fill in later): ")
    if key == "":
        import os
        key = os.urandom(32)
        print(f"Generated key: {key}")
    with open(path, 'w') as f:
        f.write(f"# You will want this to be readable only by the server, not the client, naturally\nSHARED_SECRET = b'{key}'")