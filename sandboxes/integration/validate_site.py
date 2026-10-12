#!/usr/bin/env python3
"""Check the running built site image and write a candidate validation receipt."""
import json
import os
import re
from hashlib import sha256
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import urlopen

BASE = os.environ.get('SITE_BASE_URL', 'http://127.0.0.1:18085')
CHECKS = {
    '/sliding-tasks/': 'Make room for',
    '/sliding-tasks/pt-br/': '<html lang="pt-BR">',
    '/sliding-tasks/privacy/': 'Privacy policy',
    '/sliding-tasks/pt-br/privacy/': 'Política de privacidade',
    '/sliding-tasks/styles.css': '--orange:',
    '/sliding-tasks/favicon.svg': '<svg',
    '/health/live': 'live',
    '/health/ready': 'ready',
}

def validate():
    stylesheets = set()
    for path, expected in CHECKS.items():
        with urlopen(BASE + path, timeout=10) as response:
            assert response.status == 200, path
            body = response.read().decode()
            assert expected in body, path
            if path.startswith('/sliding-tasks/') and path.endswith('/'):
                stylesheet = re.search(
                    r'<link rel="stylesheet" href="(/sliding-tasks/styles\.([0-9a-f]{64})\.css)"',
                    body,
                )
                assert stylesheet, f'{path}: missing content-hashed stylesheet'
                stylesheets.add(stylesheet.groups())
    assert len(stylesheets) == 1, 'All pages must use the same stylesheet'
    stylesheet_path, stylesheet_hash = stylesheets.pop()
    with urlopen(BASE + stylesheet_path, timeout=10) as response:
        assert response.status == 200, stylesheet_path
        assert response.headers.get_content_type() == 'text/css', stylesheet_path
        stylesheet_body = response.read()
        assert sha256(stylesheet_body).hexdigest() == stylesheet_hash, stylesheet_path
    previous_stylesheet = '/sliding-tasks/styles.' + '0' * 64 + '.css'
    with urlopen(BASE + previous_stylesheet, timeout=10) as response:
        assert response.status == 200, previous_stylesheet
        assert response.headers.get_content_type() == 'text/css', previous_stylesheet
        assert response.read() == stylesheet_body, previous_stylesheet
    for path in ('/', '/unrelated'):
        try:
            urlopen(BASE + path, timeout=10)
        except HTTPError as error:
            assert error.code == 404, path
        else:
            raise AssertionError(f'{path} must return 404')
    receipt = {'status': 'passed', 'source_revision': os.environ['GITHUB_SHA'],
               'checks': [*CHECKS, stylesheet_path, previous_stylesheet], 'unrelated_paths': '404',
               'image_reference': os.environ.get('IMAGE_REFERENCE')}
    Path('site-validation.json').write_text(json.dumps(receipt, indent=2) + '\n')

if __name__ == '__main__':
    validate()
