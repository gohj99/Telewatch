//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2021
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
#pragma once

#include <cstddef>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif
void telegram_rand_bytes(uint8_t *buffer, size_t length);
void telegram_sha1(/*const*/ uint8_t *msg, size_t length, uint8_t *output);
void telegram_sha256(/*const*/ uint8_t *msg, size_t length, uint8_t *output);
void telegram_aes_ige_encrypt(/*const*/ uint8_t *in, uint8_t *out, size_t length,
                              uint8_t *key, uint8_t *iv);
void telegram_aes_ige_decrypt(/*const*/ uint8_t *in, uint8_t *out, size_t length,
                              uint8_t *key, uint8_t *iv);
void telegram_aes_ctr_encrypt(uint8_t *inout, size_t length, /*const*/ uint8_t *key,
                              uint8_t *iv, uint8_t *ecount, uint32_t *num);
#ifdef __cplusplus
}
#endif
