import feedparser
import requests
from bs4 import BeautifulSoup
import time

# RSS 피드 파싱
feed = feedparser.parse('https://learningenglish.voanews.com/api/zbmroml-vomx-tpeqboo_')

testlink = 'http://localhost:8080/api/rss'
response = requests.get(testlink).json()
for entry in response:
    print('https://learningenglish.voanews.com'+entry['url'])


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
        if article_title:
            print(f"\n========제목========")
            print(article_title.get_text(strip=True))

        # 본문 추출 - wsw 클래스 사용
        article_body = soup.find('div', class_='wsw')

        if article_body:
            # 본문 내 모든 p 태그 수집
            paragraphs = article_body.find_all('p')
            content = '\n'.join([p.get_text(strip=True) for p in paragraphs])

            print(f"\n========본문========")
            print(content)
        else:
            print("본문을 찾을 수 없습니다.")

    # 서버 부하 방지
    time.sleep(1)
