import os
import re

base_dir = r"c:\Users\USER\Desktop\sleepy-backend\sleepy-backend\src\main\java\com\sleepyproject\sleepy_backend\service"

for root, _, files in os.walk(base_dir):
    for file in files:
        if file.endswith("Service.java") and file != "MemberService.java":
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            if 'memberRepository.findByUsername(username)' in content:
                # 1. Add import
                if 'import com.sleepyproject.sleepy_backend.service.member.MemberReader;' not in content:
                    content = content.replace('import org.springframework.stereotype.Service;', 
                                              'import org.springframework.stereotype.Service;\nimport com.sleepyproject.sleepy_backend.service.member.MemberReader;')
                
                # 2. Add MemberReader dependency
                if 'private final MemberReader memberReader;' not in content:
                    content = re.sub(r'(private final [^;]+;)', r'\1\n    private final MemberReader memberReader;', content, count=1)

                # 3. Replace memberRepository.findByUsername(username) up to the next semicolon
                content = re.sub(r'memberRepository\.findByUsername\(username\)[^;]+;', 'memberReader.getMember(username);', content)

                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Refactored: {file}")
