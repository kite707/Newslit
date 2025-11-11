import feedparser
import requests
from bs4 import BeautifulSoup
import time
from datetime import datetime

# RSS 피드 파싱
feed = feedparser.parse('https://learningenglish.voanews.com/api/zbmroml-vomx-tpeqboo_')

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
        
        # 날짜 포맷 변환 (yyyy-MM-dd 형식으로)
        formatted_date = None
        if pub_date_str:
            try:
                # ISO 8601 형식 파싱 (예: 2024-11-10T15:30:00Z)
                dt = datetime.fromisoformat(pub_date_str.replace('Z', '+00:00'))
                formatted_date = dt.strftime('%Y-%m-%d')
            except:
                try:
                    # 다른 형식 시도
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
        
        # API로 POST 요청
        try:
            api_response = requests.post(
                'http://localhost:8080/api/article',
                json=article_data,
                headers={'Content-Type': 'application/json'}
            )
            
            if api_response.status_code in [200, 201]:
                print(f"\n========API 전송 성공========")
                print(f"상태 코드: {api_response.status_code}")
            else:
                print(f"\n========API 전송 실패========")
                print(f"상태 코드: {api_response.status_code}")
                print(f"응답: {api_response.text}")
        except Exception as e:
            print(f"\n========API 전송 오류========")
            print(f"오류: {str(e)}")
    
    # 서버 부하 방지
    time.sleep(1)

print("\n========모든 작업 완료========")
