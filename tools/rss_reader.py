import feedparser
import requests
from bs4 import BeautifulSoup
import time
from datetime import datetime
import re

# RSS 피드 파싱
feed = feedparser.parse('https://learningenglish.voanews.com/api/zbmroml-vomx-tpeqboo_')

# 각 아이템에서 링크 추출 및 크롤링
for entry in feed.entries:
    title = entry.title
    link = entry.link

    print(f"\n========링크========")
    print(link)

    # 웹 페이지 요청
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
    response = requests.get(link, headers=headers)

    if response.status_code == 200:
        # HTML 파싱
        soup = BeautifulSoup(response.content, 'html.parser')

        # 제목 추출
        article_title = soup.find('h1', class_='title pg-title')
        extracted_title = article_title.get_text(strip=True) if article_title else title
        print(f"\n========제목========")
        print(extracted_title)

        # 발행일 추출
        pub_date_str = None
        pub_date = soup.find('meta', {'name': 'pubdate'})
        if pub_date and pub_date.get('content'):
            pub_date_str = pub_date.get('content')
        else:
            pub_date_time = soup.find('time')
            if pub_date_time:
                pub_date_str = pub_date_time.get('datetime') or pub_date_time.get_text(strip=True)

        print(f"\n========날짜========")
        print(pub_date_str)

        # 본문 추출
        article_body = soup.find('div', class_='wsw')
        content = ""

        if article_body:
            words_h2 = article_body.find('h2', class_='wsw__h2')
            paragraphs = []

            for elem in article_body.children:
                if elem.name == 'h2' and 'wsw__h2' in elem.get('class', []):
                    break
                if elem.name == 'p':
                    text = elem.get_text(strip=True)
                    if text:
                        paragraphs.append(text)

            content = '\n\n'.join(paragraphs)
            print(f"\n========본문========")
            print(content)
        else:
            print("본문을 찾을 수 없습니다.")
            continue

        # 단어 추출
        print(f"\n========단어========")
        words_h2 = soup.find('h2', class_='wsw__h2')
        words_list = []

        if words_h2:
            for sibling in words_h2.find_next_siblings():
                if sibling.name == 'p':
                    text = sibling.get_text(strip=True)
                    if text and text != 'Forum':
                        words_list.append(text)

            if words_list:
                print('\n'.join(words_list))
            else:
                print("단어가 없습니다.")
        else:
            print("단어 섹션이 없습니다.")

        # 날짜 포맷 변환
        formatted_date = None
        if pub_date_str:
            try:
                dt = datetime.fromisoformat(pub_date_str.replace('Z', '+00:00'))
                formatted_date = dt.strftime('%Y-%m-%d')
            except:
                try:
                    dt = datetime.strptime(pub_date_str, '%Y-%m-%d')
                    formatted_date = pub_date_str
                except:
                    print(f"날짜 파싱 실패: {pub_date_str}")

        # API로 전송할 데이터 구성
        article_data = {
            "title": extracted_title,
            "originalText": content,
            "sourceUrl": link,
            "publishedDate": formatted_date,
            "source": "VOA Learning English"
        }

        # Article API로 POST 요청
        article_id = None
        try:
            api_response = requests.post(
                'http://localhost:8080/api/article',
                json=article_data,
                headers={'Content-Type': 'application/json'}
            )

            if api_response.status_code in [200, 201]:
                print(f"\n========Article API 전송 성공========")
                print(f"상태 코드: {api_response.status_code}")

                # 응답에서 article ID 추출
                response_data = api_response.json()
                article_id = response_data.get('id')
                print(f"Article ID: {article_id}")
            else:
                print(f"\n========Article API 전송 실패========")
                print(f"상태 코드: {api_response.status_code}")
                print(f"응답: {api_response.text}")
        except Exception as e:
            print(f"\n========Article API 전송 오류========")
            print(f"오류: {str(e)}")

        # Article이 성공적으로 저장되고 단어 목록이 있으면 Vocabulary 저장
        if article_id and words_list:
            print(f"\n========Vocabulary 저장 시작========")

            for word_text in words_list:
                # 단어 파싱: "word – definition" 형식 또는 "word (part of speech) – definition" 형식
                # 예: "conduct – (v.) to organize and direct" 또는 "conduct – to organize"

                # 기본값 설정
                word = word_text
                part_of_speech = "NOUN"  # 기본값

                # "–" 또는 "-" 기준으로 단어와 정의 분리
                if '–' in word_text:
                    word = word_text.split('–')[0].strip()
                elif '-' in word_text:
                    word = word_text.split('-')[0].strip()

                # 품사 추출 (괄호 안에 있는 경우)
                pos_match = re.search(r'\(([nvadj\.]+)\)', word)
                if pos_match:
                    pos_abbr = pos_match.group(1).replace('.', '').strip().lower()
                    # 품사 매핑
                    pos_mapping = {
                        'n': 'NOUN',
                        'v': 'VERB',
                        'adj': 'ADJECTIVE',
                        'adv': 'ADVERB',
                        'prep': 'PREPOSITION',
                        'conj': 'CONJUNCTION',
                        'pron': 'PRONOUN',
                        'interj': 'INTERJECTION'
                    }
                    part_of_speech = pos_mapping.get(pos_abbr, 'NOUN')
                    # 괄호 부분 제거
                    word = re.sub(r'\s*\([^)]*\)', '', word).strip()

                # Vocabulary 데이터 구성
                vocabulary_data = {
                    "articleId": article_id,
                    "word": word,
                    "partOfSpeech": part_of_speech
                }

                # Vocabulary API로 POST 요청
                try:
                    vocab_response = requests.post(
                        'http://localhost:8080/api/vocabulary',
                        json=vocabulary_data,
                        headers={'Content-Type': 'application/json'}
                    )

                    if vocab_response.status_code in [200, 201]:
                        print(f"✓ 단어 저장 성공: {word} ({part_of_speech})")
                    else:
                        print(f"✗ 단어 저장 실패: {word}")
                        print(f"  상태 코드: {vocab_response.status_code}")
                        print(f"  응답: {vocab_response.text}")
                except Exception as e:
                    print(f"✗ 단어 저장 오류: {word}")
                    print(f"  오류: {str(e)}")

                # API 부하 방지
                time.sleep(0.3)

    # 서버 부하 방지
    time.sleep(1)

print("\n========모든 작업 완료========")
