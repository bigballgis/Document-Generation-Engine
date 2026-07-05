"""Real-bank-grade demo letter generation — drive the full pipeline through API management.

Usage:
    python .tmp/demo_driver.py inspect                 # show current state
    python .tmp/demo_driver.py publish <externalId>    # advance template to PUBLISHED + policy
    python .tmp/demo_driver.py generate <externalId>   # create credential + runtime generate
"""
import json, os, sys, time, urllib.request, urllib.error

BASE = os.environ.get("BACKEND_URL", "http://localhost:8080")
MGMT = BASE + "/api/management/v1"
RUNTIME = BASE + "/api/dev/v1"

USERS = {
    "admin":      ("10000001", "ChangeMe123!"),
    "group_admin":("10000002", "ChangeMe123!"),
    "author":     ("10000003", "ChangeMe123!"),
}

def http(method, url, token=None, body=None, multipart=None, raw=False):
    headers = {}
    data = None
    if token: headers["Authorization"] = "Bearer " + token
    if multipart:
        # multipart via http.client is fiddly; shell out to curl
        return curl_multipart(method, url, headers, multipart)
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw_bytes = r.read()
            if raw: return r.status, dict(r.headers), raw_bytes
            if not raw_bytes: return r.status, None, None
            try: return r.status, dict(r.headers), json.loads(raw_bytes)
            except: return r.status, dict(r.headers), raw_bytes.decode("utf-8","replace")
    except urllib.error.HTTPError as e:
        raw_bytes = e.read()
        try: err = json.loads(raw_bytes)
        except: err = raw_bytes.decode("utf-8","replace")
        return e.code, dict(e.headers), err

def curl_multipart(method, url, headers, fields):
    import subprocess
    args = ["curl","-sS","-X",method,url]
    for k,v in headers.items():
        args += ["-H", f"{k}: {v}"]
    for k,v in fields.items():
        if isinstance(v, tuple):  # (filepath)
            args += ["-F", f"{k}=@{v[0]}"]
        else:
            args += ["-F", f"{k}={v}"]
    r = subprocess.run(args, capture_output=True, text=True)
    try: return 200, {}, json.loads(r.stdout)
    except: return 0, {}, r.stdout

def login(role="group_admin"):
    u,p = USERS[role]
    s,_,b = http("POST", MGMT+"/auth/login", body={"username":u,"password":p})
    assert s==200 and b, f"login {role} failed: {s} {b}"
    return b["result"]["accessToken"]

def get_json(path, token):
    s,h,b = http("GET", MGMT+path, token=token)
    return s, b

def main():
    cmd = sys.argv[1] if len(sys.argv)>1 else "inspect"
    if cmd=="inspect":
        ga = login("group_admin")
        s, b = get_json("/templates?size=200", ga)
        print(f"templates (status {s}):")
        if s==200:
            for t in b["result"]:
                print(f"  {t.get('externalId','?'):32} | {t.get('lifecycleStatus','?'):14} | {t.get('groupCode','?'):8} | {t.get('name','')[:50]}")
        else:
            print(b)
        s, b = get_json("/masters?size=200", ga)
        print(f"masters (status {s}):")
        if s==200:
            for m in b["result"]:
                print(f"  {m.get('name','?'):48} | {m.get('reviewState','?'):10} | {m.get('groupCode','?'):8}")
        else:
            print(b)
    elif cmd=="generate":
        ext = sys.argv[2]
        ga = login("group_admin")
        # find template
        s,b = get_json("/templates?size=200", ga)
        tpl = next((t for t in b["result"] if t["externalId"]==ext), None)
        assert tpl, f"template {ext} not found"
        tid = tpl["id"]
        print(f"template: {ext} id={tid} status={tpl.get('lifecycleStatus')}")
        # create credential
        s,_,b = http("POST", MGMT+f"/templates/{tid}/api/credentials", token=ga)
        print(f"credential create: {s} {b}")
        cred = b["result"]
        # generate via runtime
        headers = {
            "X-Api-Credential-Id": cred["externalId"],
            "X-Api-Credential-Secret": cred["secret"],
            "X-Access-Account": "demo-real-letter-caller",
            "Content-Type": "application/json",
        }
        body = {
            "output": {"format":"DOCX","mode":"SYNC_STREAM"},
            "variables": {"customerName":"Northgate Manufacturing Ltd."} if "CREDIT" in ext else {"borrowerName":"Mr Oliver Hartley"},
            "requestId": f"req-demo-{int(time.time())}",
            "idempotencyKey": f"idem-demo-{int(time.time())}",
        }
        req = urllib.request.Request(f"{RUNTIME}/templates/{ext}/default/generate",
            data=json.dumps(body).encode(), method="POST", headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                doc = r.headers.get("documentId")
                print(f"GENERATE status={r.status} documentId={doc}")
                print("headers:", {k:v for k,v in r.headers.items()})
                if doc:
                    with open(f".tmp/generated_{ext}.docx","wb") as f: f.write(r.read())
                    print("saved .tmp/generated_"+ext+".docx")
        except urllib.error.HTTPError as e:
            print("GENERATE FAILED", e.code, e.read()[:500])

if __name__=="__main__":
    main()
