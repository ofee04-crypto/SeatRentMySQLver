package com.example.backend.controller.rec;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.rec.RecRent;
import com.example.backend.model.rec.RentDetails;
import com.example.backend.model.spot.Seat;
import com.example.backend.repository.rec.RecRentRepository;
import com.example.backend.service.rec.RecDetailMgnService;
import com.example.backend.service.spot.SeatService;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/rec-rent") // 1. 將根路徑改為 /rec-rent
@CrossOrigin
@Transactional
public class RecRentController {

    @Autowired // SQL view 的查詢
    private RecDetailMgnService recDetailService;

    @Autowired // 實際的訂單TABLE
    private RecRentRepository rentRepos;

    @Autowired // 座位 TABLE 的操作服務
    private SeatService seatService;

    // 1. 查詢全部或依條件搜尋
    @GetMapping
    public List<RentDetails> searchRents(
            @RequestParam(required = false) Integer recSeqId,
            @RequestParam(required = false) String recId,
            @RequestParam(required = false) Integer memId,
            @RequestParam(required = false) String memName,
            @RequestParam(required = false) String recStatus,
            @RequestParam(required = false) Integer spotId,
            @RequestParam(required = false) String spotName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 如果所有參數都為空，則返回所有列表
        if (recId == null && memId == null && memName == null && recStatus == null && spotId == null && spotName == null
                && startDate == null && endDate == null) {
            return recDetailService.getAllRec();
        }

        // 否則，呼叫 search service
        return recDetailService.search(recSeqId, recId, memId, memName, recStatus, spotId, spotName, startDate,
                endDate);
    }

    // 2. 依訂單PK (recSeqId) 查詢
    @GetMapping("/{id}")
    public RentDetails getRecById(@PathVariable String id) {
        return recDetailService.getRecById(id);
    }

    // 4. 新增訂單
    @PostMapping("/new") // 2. 將端點改為 /new
    public ResponseEntity<RecRent> create(@RequestBody RecRent recRent) {
        // 3. 在後端控制台打印收到的完整訂單資料
        System.out.println("新訂單請求: " + recRent.toString());

        // recSeqId 會由資料庫自動產生
        // recId 會由資料庫自動計算

        // 說明: 若前端未提供租借時間，則設定為當前伺服器時間；若有提供則使用前端值
        if (recRent.getRecRentDT2() == null) {
            recRent.setRecRentDT2(LocalDateTime.now());
        }

        RecRent savedRent = rentRepos.save(recRent);

        // 租借訂單建立成功後，同步更新該座位的 spotId 為 0 (表示被租用，不在任何可選地點上)
        // 由於整個 Controller 已標記為 @Transactional，此操作將與上面的訂單新增在同一個資料庫交易中，確保資料一致性。
        Integer seatId = Integer.valueOf(savedRent.getSeatsId());

        Seat seatToUpdate = seatService.selectById(seatId);
        if (seatToUpdate != null) {
            seatToUpdate.setSpotId(null);
            seatService.update(seatToUpdate);

        }

        // 4. 返回 201 Created 狀態碼以及已儲存的訂單物件
        return new ResponseEntity<>(savedRent, HttpStatus.CREATED);
    }

    // 5. 依訂單業務ID (recId) 更新
    @PutMapping("/{recId}")
    public ResponseEntity<RecRent> update(
            @PathVariable String recId,
            @RequestBody RecRent updatedRentData,
            @RequestParam(required = false) Integer spotIdReturn) { // [新增] 接收 URL 參數作為備案

        // [新增] 除錯日誌：確認前端傳來的資料是否包含 spotIdReturn
        System.out.println("收到訂單更新請求 RecId: " + recId);
        System.out.println("前端傳入的歸還站點 (Body): " + updatedRentData.getSpotIdReturn() + ", (Param): " + spotIdReturn);

        // 使用 recId 從資料庫中找到對應的 RecRent 實體
        RecRent existingRent = rentRepos.findByRecId(recId);

        // 如果訂單不存在，返回 404 Not Found
        if (existingRent == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        System.out.println("getRecRentDT2(): " + updatedRentData.getRecStatus());
        System.out.println("updatedgetRecRentDT2(): " + updatedRentData.getRecRentDT2());
        System.out.println(" existgetRecRentDT2(): " + existingRent.getRecRentDT2());
        System.out.println("updatedgetRecReturnDT2(): " + updatedRentData.getRecReturnDT2());
        System.out.println(" existgetRecReturnDT2(): " + existingRent.getRecReturnDT2());

        // 說明: 為確保資料一致性與安全性，僅更新允許修改的欄位。
        // 訂單成立時的核心資訊，如會員ID、座位ID、租借場地與租借時間，在此方法中不應被修改。
        // 說明: 更新還車場地，僅在前端提供時更新
        // [修正] 優先使用 Body 中的值，若為 null 則嘗試使用 URL Param 的值 (雙重保險)

        // 說明: 更新訂單狀態
        String newStatus = updatedRentData.getRecStatus();
        if (newStatus != null) {
            existingRent.setRecStatus(newStatus);
        }

        Integer finalSpotIdReturn = updatedRentData.getSpotIdReturn();
        if (updatedRentData.getSpotIdReturn() == null) {
            finalSpotIdReturn = spotIdReturn;
        } else {
            existingRent.setSpotIdReturn(finalSpotIdReturn);
        }

        // 說明: 更新其餘可變動的訂單資訊，僅在前端提供非null值時才更新
        if (updatedRentData.getRecRentDT2() != null) {
            existingRent.setRecRentDT2(updatedRentData.getRecRentDT2());
        }
        if (updatedRentData.getSpotIdRent() != null) {
            existingRent.setSpotIdRent(updatedRentData.getSpotIdRent());
        }

        if (updatedRentData.getRecUsageDT2() != null) {
            existingRent.setRecUsageDT2(updatedRentData.getRecUsageDT2());
        }
        if (updatedRentData.getRecPrice() != null) {
            existingRent.setRecPrice(updatedRentData.getRecPrice());
        }
        if (updatedRentData.getRecRequestPay() != null) {
            existingRent.setRecRequestPay(updatedRentData.getRecRequestPay());
        }
        if (updatedRentData.getRecPayment() != null) {
            existingRent.setRecPayment(updatedRentData.getRecPayment());
        }
        if (updatedRentData.getRecPayBy() != null) {
            existingRent.setRecPayBy(updatedRentData.getRecPayBy());
        }
        if (updatedRentData.getRecInvoice() != null) {
            existingRent.setRecInvoice(updatedRentData.getRecInvoice());
        }
        if (updatedRentData.getRecCarrier() != null) {
            existingRent.setRecCarrier(updatedRentData.getRecCarrier());
        }
        if (updatedRentData.getRecViolatInt() != null) {
            existingRent.setRecViolatInt(updatedRentData.getRecViolatInt());
        }
        if (updatedRentData.getRecNote() != null) {
            existingRent.setRecNote(updatedRentData.getRecNote());
        }

        // 注意：任何 RecRent 中存在但前端未提供的欄位，在此處將保持其在資料庫中的原始值

        // 保存更新後的訂單物件，JPA 將會執行 UPDATE 操作
        RecRent savedRent = rentRepos.save(existingRent);

        // 如果前端狀態更新為「已歸還」但沒有提供歸還時間
        // 則自動將歸還時間設為當前伺服器時間。
        if (updatedRentData.getRecReturnDT2() != null) {
            existingRent.setRecReturnDT2(updatedRentData.getRecReturnDT2());
        } else if ("已完成".equals(newStatus)) { // 請注意: "2" 是假設的「已歸還」狀態代碼，請依據您系統的實際定義修改。
            existingRent.setRecReturnDT2(LocalDateTime.now());
        }

        // 同步更新該座位的地點(spotId)為歸還時的地點。
        Integer newSpotId = finalSpotIdReturn; // [修正] 使用整合後的變數
        if ("已完成".equals(newStatus) && newSpotId != null) {
            Integer seatId = Integer.valueOf(savedRent.getSeatsId());
            if (seatId != null) {
                Seat seatToUpdate = seatService.selectById(seatId);
                if (seatToUpdate != null) {
                    seatToUpdate.setSpotId(newSpotId);
                    seatService.update(seatToUpdate);
                }
            }
        }

        // 返回 200 OK 以及更新後的訂單物件
        return new ResponseEntity<>(savedRent, HttpStatus.OK);
    }

    // 6. 依訂單業務ID (recId) 刪除
    @DeleteMapping("/{recId}")
    public ResponseEntity<Void> delete(@PathVariable String recId) { // 檢查訂單是否存在
        if (recDetailService.getRecById(recId) == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // 呼叫 repository 依 recId 刪除
        rentRepos.deleteByRecId(recId);
        // 返回 204 No Content 表示成功刪除
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}