async function loginAndGetToken(request, { username, password }) {
  const res = await request.post('/auth/login', {
    data: { username, password },
  });
  if (!res.ok()) {
    throw new Error(`Login failed for ${username}: ${res.status()}`);
  }
  const body = await res.json();
  return body.token;
}

async function authedGet(request, url, token) {
  return request.get(url, { headers: { Authorization: token } });
}

async function authedPatch(request, url, token, data) {
  return request.patch(url, {
    headers: { Authorization: token, 'Content-Type': 'application/json' },
    data,
  });
}

async function authedPut(request, url, token, data) {
  return request.put(url, {
    headers: { Authorization: token, 'Content-Type': 'application/json' },
    data,
  });
}

module.exports = {
  loginAndGetToken,
  authedGet,
  authedPatch,
  authedPut,
};
