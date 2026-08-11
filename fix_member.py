import os
import re

base_dir = r"c:\Users\USER\Desktop\sleepy-backend\sleepy-backend\src\main\java\com\sleepyproject\sleepy_backend\service"

for root, _, files in os.walk(base_dir):
    for file in files:
        if file.endswith("Service.java") and file != "MemberService.java":
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            # Remove trailing .orElseThrow(...)
            # The pattern is .orElseThrow(() -> new SomethingException("..."));
            # We want to remove .orElseThrow(...) entirely up to the semicolon
            # But wait, it might not be a semicolon if it's chained. But Member has no chained methods here.
            # Example: memberReader.getMember(username)\n .orElseThrow(() -> new IllegalArgumentException("..."))
            
            # Use regex to remove .orElseThrow(...)
            new_content = re.sub(r'\.orElseThrow\([^\)]+\)\s*->\s*new\s+[a-zA-Z]+\([^\)]*\)\)', '', content, flags=re.DOTALL)
            
            # Since the lambda might have nested parens, a simpler approach:
            # We know exactly what follows memberReader.getMember(username):
            # \s*\.orElseThrow\([^\)]+\)\s*->\s*new\s+[^\)]+\)\)
            
            # Let's just remove anything from .orElseThrow to the next semicolon, IF it's after getMember
            
            new_content = re.sub(r'\.orElseThrow\([^;]+;', ';', content)
            
            # Wait, LikeService.java:53
            # Member member = memberReader.getMember(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            # This will become Member member = memberReader.getMember(username);
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Fixed: {file}")
