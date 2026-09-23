// M4/M5 的后端接口目前还没接通（队友还在开发中），所以 researcher/ 和
// participant/ 下每个 api/*Api.ts 文件都会根据 USE_MOCK_API 分支：函数签名
// 不变，但 mock 分支返回内存里的假数据而不是真正发请求。
//
// 等某个模块的真实接口上线后，把该模块的 mock 分支和 mock 数据文件删掉即可，
// 其他地方不用动，因为调用方永远只 import *Api.ts 里的函数，不会直接
// import mock 文件。
export const USE_MOCK_API = true

// 让 mock 的返回也带点延迟，感觉更像真实网络请求（这样加载态、骨架屏之类
// 的表现在 mock 模式和接入真实后端后是一致的）。
export function mockDelay(ms = 300): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
