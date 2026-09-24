const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

function load(fetch) {
  const source = readFileSync(path.join(__dirname, "../src/lib/admin-jobs.ts"), "utf8");
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const exports = {};
  vm.runInNewContext(code, {
    exports, URLSearchParams,
    require: (name) => {
      if (name === "@/lib/api-client") return { apiFetch: fetch };
      if (name === "@/lib/admin-constants") return { ADMIN_DEFAULT_PAGE_SIZE: 50, ADMIN_MAX_PAGE_SIZE: 100 };
      throw new Error(`Unexpected dependency ${name}`);
    },
  });
  return exports;
}

test("forwards every visible filter and uses backend total", async () => {
  const api = load(async (url, options) => {
    const params = new URL(url, "http://test").searchParams;
    assert.deepEqual(Object.fromEntries(params), { status: "pending", type: "award_badges", dedupeKey: "key:%",
      userId: "u1", q: "error & retry", page: "2", limit: "10" });
    assert.equal(options.cache, "no-store");
    return { jobs: [{ id: "job" }], total: 31 };
  });
  const result = await api.getPaginatedJobs({ page: 2, pageSize: 10, status: " pending ", type: "award_badges",
    dedupeKey: "key:%", userId: "u1", q: "error & retry" });
  assert.equal(result.items[0].id, "job");
  assert.equal(result.pagination.total, 31);
  assert.equal(result.pagination.pageCount, 4);
});

test("bounds pagination and omits blank filters", async () => {
  const api = load(async (url) => {
    const params = new URL(url, "http://test").searchParams;
    assert.equal(params.has("q"), false);
    assert.ok(Number(params.get("page")) >= 1);
    assert.ok(Number(params.get("limit")) >= 1 && Number(params.get("limit")) <= 100);
    assert.ok((Number(params.get("page")) - 1) * Number(params.get("limit")) <= 2147483647);
    return { jobs: [], total: 0 };
  });
  for (const page of [-1, NaN, Infinity, 1.5, Number.MAX_VALUE]) {
    await api.getPaginatedJobs({ page, pageSize: 1000, q: "  " });
  }
  assert.equal((await api.getPaginatedJobs({ pageSize: NaN })).pagination.pageSize, 50);
});

test("backend errors propagate instead of becoming an empty successful list", async () => {
  const failure = new Error("database unavailable");
  const api = load(async () => { throw failure; });
  await assert.rejects(api.getPaginatedJobs({}), (error) => error === failure);
});
