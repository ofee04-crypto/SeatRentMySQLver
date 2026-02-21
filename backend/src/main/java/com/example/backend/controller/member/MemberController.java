package com.example.backend.controller.member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.member.Member;
import com.example.backend.service.member.MemberService;

@CrossOrigin(origins = "http://localhost:5173") // 確保跨域沒問題
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 查全部
    @GetMapping
    public List<Member> findAll() {
        return memberService.findAll();
    }

    // 查單筆
    @GetMapping("/find")
    public ResponseEntity<?> findOne(@RequestParam(required = false) Integer memId) {
        if (memId == null) {
            return ResponseEntity.badRequest().body("請輸入 memId");
        }

        Member member = memberService.findById(memId);

        if (member != null) {
            // --- 暴力拆解法：只傳需要的欄位，避開所有可能導致 500 的複雜物件 ---
            Map<String, Object> response = new HashMap<>();
            response.put("memId", member.getMemId());
            response.put("memPoints", member.getMemPoints());
            response.put("memName", member.getMemName());
            response.put("memUsername", member.getMemUsername());
            response.put("memEmail", member.getMemEmail());
            response.put("memPhone", member.getMemPhone());
            response.put("memInvoice", member.getMemInvoice());
            response.put("memImage", member.getMemImage());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body("查無此會員");
        }
    }

    // 新增
    @PostMapping
    public ResponseEntity<?> insert(@RequestBody Member member) {
        try {
            memberService.insert(member);
            return ResponseEntity.ok("會員新增成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 修改
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody Member member) {

        // 根據 ID 找到舊資料 (這步很重要，確保是更新現有資料庫資料)
        Member old = memberService.findById(member.getMemId());
        if (old == null) {
            return ResponseEntity.status(404).body("查無此會員");
        }

        // 將前端傳來的資料填入 old 物件
        old.setMemName(member.getMemName());
        old.setMemEmail(member.getMemEmail());
        old.setMemPhone(member.getMemPhone());
        old.setMemInvoice(member.getMemInvoice());
        old.setMemUsername(member.getMemUsername());
        old.setMemPoints(member.getMemPoints());

        // 呼叫 Service 更新 (只呼叫這一個 update 即可，它會處理存檔)
        try {
            // 這裡會同時處理基本資料存檔與密碼驗證
            memberService.update(old, member.getMemPassword());
        } catch (IllegalArgumentException e) {
            // 如果密碼格式不對，回傳錯誤訊息給前端
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("會員修改成功");
    }

    // 刪除
    @GetMapping("/delete")
    public String delete(@RequestParam Integer memId) {
        memberService.deleteById(memId);
        return "會員刪除成功（memId=" + memId + "）";
    }

    // 模糊查詢
    @GetMapping("/search")
    public Object search(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "請輸入搜尋關鍵字";
        }
        return memberService.search(keyword);
    }

    // 會員註冊
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            memberService.insert(member);
            return ResponseEntity.ok("註冊成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 1. 停權 API (對應前端的 confirmBanMember)
    @PostMapping("/ban")
    public ResponseEntity<?> banMember(@RequestBody Map<String, Object> payload) {
        try {
            // 從前端傳來的 JSON 取得 memId (前端傳的是 { memId: xxx, reason: yyy })
            Integer memId = (Integer) payload.get("memId");

            // 找到該會員
            Member member = memberService.findById(memId);
            if (member == null) {
                return ResponseEntity.status(404).body("找不到該會員");
            }

            // 將狀態設為 0 (停權)
            member.setMemStatus(0);

            // 儲存變更 (直接用你現有的 update 方法，密碼傳 null 代表不改密碼)
            memberService.update(member, null);

            return ResponseEntity.ok("會員已成功停權");
        } catch (Exception e) {
            e.printStackTrace(); // 在後端終端機印出錯誤細節
            return ResponseEntity.status(500).body("停權失敗：" + e.getMessage());
        }
    }

    // 2. 重新啟用 API (對應前端的 activateMember)
    @PostMapping("/activate")
    public ResponseEntity<?> activateMember(@RequestBody Map<String, Object> payload) {
        try {
            Integer memId = (Integer) payload.get("memId");

            Member member = memberService.findById(memId);
            if (member == null) {
                return ResponseEntity.status(404).body("找不到該會員");
            }

            // 將狀態設為 1 (啟用)
            member.setMemStatus(1);

            // 儲存變更
            memberService.update(member, null);

            return ResponseEntity.ok("會員已重新啟用");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("啟用失敗：" + e.getMessage());
        }
    }

}