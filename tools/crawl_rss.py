import requests
from bs4 import BeautifulSoup
import json


def crawl_voa_rss_links_simple():
    """requests + BeautifulSoup으로 먼저 시도"""
    url = 'https://learningenglish.voanews.com/rssfeeds'

    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }

        print(f"페이지 로딩 중 (Simple Mode): {url}")
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()

        soup = BeautifulSoup(response.text, 'html.parser')
        links = soup.find_all('a', class_='link-service')

        if len(links) > 0:
            print(f"✅ Simple Mode 성공! {len(links)}개의 링크 발견\n")
            return links, soup
        else:
            print("⚠️ Simple Mode 실패 - Playwright 필요")
            return None, None

    except Exception as e:
        print(f"Simple Mode 오류: {e}")
        return None, None


def process_simple_mode(links, soup):
    """Simple Mode로 가져온 링크 처리"""
    rss_data = []

    for idx, link in enumerate(links, 1):
        try:
            href = link.get('href', '')
            text = link.get_text(strip=True)

            # 제목 찾기 (h4.media-block__title)
            title = "제목 없음"
            parent = link.parent

            for _ in range(10):  # 더 많은 부모 요소 탐색
                if parent is None:
                    break

                # h4 제목 찾기
                title_elem = parent.find('h4', class_='media-block__title')
                if title_elem:
                    title = title_elem.get_text(strip=True)
                    break

                parent = parent.parent

            # 카테고리 찾기
            category = "기타"
            parent = link.parent

            for _ in range(10):
                if parent is None:
                    break
                heading = parent.find(['h2', 'h3'])
                if heading:
                    category = heading.get_text(strip=True)
                    break
                parent = parent.parent

            rss_data.append({
                'index': idx,
                'category': category,
                'title': title,
                'name': text,
                'url': href
            })

            print(f"{idx}. [{category}] {title}")
            print(f"   링크: {text}")
            print(f"   URL: {href}\n")

        except Exception as e:
            print(f"링크 {idx} 처리 중 오류: {e}")

    return rss_data


def save_results(rss_data):
    """결과 저장"""
    if not rss_data:
        return

    # JSON 저장
    with open('voa_rss_feeds.json', 'w', encoding='utf-8') as f:
        json.dump(rss_data, f, ensure_ascii=False, indent=2)
    print(f"\n✅ 결과가 'voa_rss_feeds.json' 파일로 저장되었습니다.")

    # 카테고리별 정리
    print("\n" + "=" * 80)
    print("카테고리별 정리")
    print("=" * 80)

    categories = {}
    for item in rss_data:
        cat = item['category']
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(item)

    for category, items in categories.items():
        print(f"\n📁 {category} ({len(items)}개)")
        print("-" * 80)
        for item in items:
            print(f"  • {item['title']}")
            print(f"    링크: {item['name']}")
            print(f"    URL: {item['url']}")

    # 마크다운 저장
    with open('voa_rss_feeds.md', 'w', encoding='utf-8') as f:
        f.write("# VOA Learning English RSS Feeds\n\n")
        for category, items in categories.items():
            f.write(f"## {category}\n\n")
            for item in items:
                f.write(f"### {item['title']}\n")
                f.write(f"- **링크 텍스트**: {item['name']}\n")
                f.write(f"- **RSS URL**: `{item['url']}`\n\n")

    print("\n✅ 마크다운 파일도 'voa_rss_feeds.md'로 저장되었습니다.")


if __name__ == "__main__":
    print("VOA Learning English RSS 피드 크롤러")
    print("=" * 80 + "\n")

    # 먼저 Simple Mode 시도
    links, soup = crawl_voa_rss_links_simple()

    if links:
        # Simple Mode 성공
        rss_data = process_simple_mode(links, soup)

    if rss_data:
        save_results(rss_data)
        print(f"\n✅ 완료! 총 {len(rss_data)}개의 RSS 피드를 수집했습니다.")
        print("\n생성된 파일:")
        print("  📄 voa_rss_feeds.json - JSON 형식")
        print("  📄 voa_rss_feeds.md - 마크다운 형식")
    else:
        print("\n⚠️ RSS 피드를 수집하지 못했습니다.")
        print("\n💡 Playwright 브라우저 설치가 필요합니다:")
        print("   playwright install chromium")
