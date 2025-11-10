import feedparser
import requests
from bs4 import BeautifulSoup
import time

# RSS 피드 파싱
feed = feedparser.parse('https://learningenglish.voanews.com/api/zbmroml-vomx-tpeqboo_')

testlink = 'http://localhost:8080/api/rss'
response = requests.get(testlink).json()
for entry in response:
    print('https://learningenglish.voanews.com' + entry['url'])

# 각 아이템에서 링크 추출 및 크롤링
for entry in feed.entries[:3]:
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
        if article_title:
            print(f"\n========제목========")
            print(article_title.get_text(strip=True))

        # 발행일 추출 - meta 태그에서 추출
        pub_date = soup.find('meta', {'name': 'pubdate'})
        if pub_date and pub_date.get('content'):
            print(f"\n========날짜========")
            print(pub_date.get('content'))
        else:
            # 대체 방법: time 태그 시도
            pub_date_time = soup.find('time')
            if pub_date_time:
                print(f"\n========날짜========")
                print(pub_date_time.get('datetime') or pub_date_time.get_text(strip=True))

        # 본문 추출 - wsw 클래스 사용
        article_body = soup.find('div', class_='wsw')

        if article_body:
            # Words in This Story h2 태그 찾기
            words_h2 = article_body.find('h2', class_='wsw__h2')

            # 본문 추출 (Words in This Story 이전까지)
            paragraphs = []
            for elem in article_body.children:
                if elem.name == 'h2' and 'wsw__h2' in elem.get('class', []):
                    break  # Words in This Story 섹션 시작하면 중단
                if elem.name == 'p':
                    text = elem.get_text(strip=True)
                    if text:
                        paragraphs.append(text)

            content = '\n\n'.join(paragraphs)
            print(f"\n========본문========")
            print(content)
        else:
            print("본문을 찾을 수 없습니다.")

        # 단어 추출
        print(f"\n========단어========")
        # Words in This Story h2 태그 찾기
        words_h2 = soup.find('h2', class_='wsw__h2')
        if words_h2:
            words_list = []
            # h2 다음의 모든 형제 요소 중 p 태그만 수집
            for sibling in words_h2.find_next_siblings():
                if sibling.name == 'p':
                    text = sibling.get_text(strip=True)
                    if text and text != 'Forum':  # Forum 텍스트 제외
                        words_list.append(text)

            if words_list:
                print('\n'.join(words_list))
            else:
                print("단어가 없습니다.")
        else:
            print("단어 섹션이 없습니다.")

    # 서버 부하 방지
    time.sleep(1)