#!/usr/bin/env python3
"""Check the running built site image and write a candidate validation receipt."""
import json
import os
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
    for path, expected in CHECKS.items():
        with urlopen(BASE + path, timeout=10) as response:
            assert response.status == 200, path
            assert expected in response.read().decode(), path
    for path in ('/', '/unrelated'):
        try:
            urlopen(BASE + path, timeout=10)
        except HTTPError as error:
            assert error.code == 404, path
        else:
            raise AssertionError(f'{path} must return 404')
    receipt = {'status': 'passed', 'source_revision': os.environ['GITHUB_SHA'],
               'checks': list(CHECKS), 'unrelated_paths': '404',
               'image_reference': os.environ.get('IMAGE_REFERENCE')}
    Path('site-validation.json').write_text(json.dumps(receipt, indent=2) + '\n')

if __name__ == '__main__':
    validate()
